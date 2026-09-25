/* Browser-only GitHub writer.
 *
 * The token is kept in memory while this page is open. For reloads it is
 * encrypted with a non-extractable Web Crypto AES-GCM key stored in IndexedDB.
 * Nothing in this file puts the token in localStorage, the URL, or article
 * content. The security boundary is the current browser origin/profile.
 */
(function () {
  "use strict";

  const DB_NAME = "mizuki-editor-vault";
  const STORE_NAME = "secrets";
  const RECORD_ID = "github-writer";
  const API_BASE = "https://api.github.com";
  const $ = (selector) => document.querySelector(selector);

  const state = {
    token: null,
    record: null,
  };

  function setStatus(selector, message, kind = "") {
    const element = $(selector);
    if (!element) return;
    element.textContent = message;
    element.className = `connection-status${kind ? ` ${kind}` : ""}`;
  }

  function openDatabase() {
    return new Promise((resolve, reject) => {
      if (!window.indexedDB) {
        reject(new Error("当前浏览器不支持 IndexedDB。"));
        return;
      }
      const request = indexedDB.open(DB_NAME, 1);
      request.onupgradeneeded = () => {
        request.result.createObjectStore(STORE_NAME, { keyPath: "id" });
      };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error || new Error("无法打开本机安全存储。"));
    });
  }

  function storeRequest(mode, callback) {
    return openDatabase().then(
      (db) =>
        new Promise((resolve, reject) => {
          const transaction = db.transaction(STORE_NAME, mode);
          const request = callback(transaction.objectStore(STORE_NAME));
          request.onsuccess = () => resolve(request.result);
          request.onerror = () => reject(request.error || new Error("本机安全存储操作失败。"));
          transaction.oncomplete = () => db.close();
          transaction.onerror = () => reject(transaction.error || new Error("本机安全存储事务失败。"));
        }),
    );
  }

  function readRecord() {
    return storeRequest("readonly", (store) => store.get(RECORD_ID));
  }

  function writeRecord(record) {
    return storeRequest("readwrite", (store) => store.put(record));
  }

  function removeRecord() {
    return storeRequest("readwrite", (store) => store.delete(RECORD_ID));
  }

  function bytesToBase64(bytes) {
    let binary = "";
    const chunkSize = 0x8000;
    for (let index = 0; index < bytes.length; index += chunkSize) {
      binary += String.fromCharCode(...bytes.subarray(index, index + chunkSize));
    }
    return btoa(binary);
  }

  function textToBase64(value) {
    return bytesToBase64(new TextEncoder().encode(value));
  }

  function base64ToBytes(value) {
    const binary = atob(value);
    const bytes = new Uint8Array(binary.length);
    for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index);
    return bytes;
  }

  async function encryptAndStore({ token, owner, repo, branch, username }) {
    if (!window.crypto?.subtle) throw new Error("当前浏览器不支持 Web Crypto。请使用 HTTPS 或 localhost。" );
    const existing = await readRecord();
    const key = existing?.key || (await crypto.subtle.generateKey(
      { name: "AES-GCM", length: 256 },
      false,
      ["encrypt", "decrypt"],
    ));
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const ciphertext = await crypto.subtle.encrypt(
      { name: "AES-GCM", iv },
      key,
      new TextEncoder().encode(token),
    );
    const record = {
      id: RECORD_ID,
      key,
      iv: Array.from(iv),
      ciphertext: Array.from(new Uint8Array(ciphertext)),
      owner,
      repo,
      branch,
      username,
      savedAt: new Date().toISOString(),
    };
    await writeRecord(record);
    state.record = record;
    state.token = token;
  }

  async function decryptRecord(record) {
    if (!record?.key || !record.iv || !record.ciphertext) return null;
    const plaintext = await crypto.subtle.decrypt(
      { name: "AES-GCM", iv: new Uint8Array(record.iv) },
      record.key,
      new Uint8Array(record.ciphertext),
    );
    return new TextDecoder().decode(plaintext);
  }

  function authHeaders(token) {
    return {
      Accept: "application/vnd.github+json",
      Authorization: `Bearer ${token}`,
      "X-GitHub-Api-Version": "2022-11-28",
    };
  }

  async function githubRequest(path, options = {}, token = state.token) {
    if (!token) throw new Error("尚未设置 GitHub 密钥。" );
    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: { ...authHeaders(token), ...(options.headers || {}) },
    });
    let payload = null;
    try {
      payload = await response.json();
    } catch {
      payload = null;
    }
    if (!response.ok) {
      const message = payload?.message || `GitHub API 返回 ${response.status}`;
      throw new Error(message);
    }
    return payload;
  }

  async function validateConnection(token, owner, repo) {
    const user = await githubRequest("/user", {}, token);
    const repository = await githubRequest(
      `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}`,
      {},
      token,
    );
    if (repository.permissions && repository.permissions.push === false) {
      throw new Error("当前 token 对这个仓库没有写入权限。" );
    }
    return { username: user.login || owner };
  }

  function fillSettings(record) {
    $("#github-owner").value = record?.owner || "";
    $("#github-repo").value = record?.repo || "";
    $("#github-branch").value = record?.branch || "master";
    $("#github-token").value = "";
  }

  async function openSettings() {
    const record = state.record || (await readRecord());
    fillSettings(record);
    $("#githubSettingsModal").classList.remove("hidden");
    const hasVault = Boolean(record && state.token);
    setStatus("#githubConnectionStatus", hasVault ? `已连接 GitHub：${record.username || record.owner}` : "尚未设置密钥。", hasVault ? "success" : "");
  }

  function closeSettings() {
    $("#githubSettingsModal").classList.add("hidden");
    $("#github-token").value = "";
  }

  async function saveSettings() {
    const owner = $("#github-owner").value.trim();
    const repo = $("#github-repo").value.trim();
    const branch = $("#github-branch").value.trim() || "master";
    const typedToken = $("#github-token").value.trim();
    const token = typedToken || state.token;
    if (!owner || !repo || !token) {
      setStatus("#githubConnectionStatus", "请填写用户名、仓库名和 token。", "error");
      return;
    }
    if (!/^[A-Za-z0-9_.-]+$/.test(owner) || !/^[A-Za-z0-9_.-]+$/.test(repo)) {
      setStatus("#githubConnectionStatus", "用户名和仓库名格式不正确。", "error");
      return;
    }
    const saveButton = $("#githubSave");
    saveButton.disabled = true;
    setStatus("#githubConnectionStatus", "正在验证 token 和仓库权限…");
    try {
      const { username } = await validateConnection(token, owner, repo);
      await encryptAndStore({ token, owner, repo, branch, username });
      $("#github-token").value = "";
      setStatus("#githubConnectionStatus", `已验证并保存。当前账号：${username}`, "success");
    } catch (error) {
      setStatus("#githubConnectionStatus", error instanceof Error ? error.message : "验证失败，请检查 token。", "error");
    } finally {
      saveButton.disabled = false;
    }
  }

  async function forgetSettings() {
    if (!window.confirm("确定清除当前浏览器保存的 GitHub 密钥吗？文章草稿不会删除。")) return;
    await removeRecord();
    state.record = null;
    state.token = null;
    fillSettings(null);
    setStatus("#githubConnectionStatus", "已清除本机密钥。", "success");
  }

  function readFrontMatterTitle(content) {
    const match = content.match(/^---\s*\n([\s\S]*?)\n---/);
    if (!match) return "";
    const titleLine = match[1].split("\n").find((line) => /^title\s*:/.test(line));
    if (!titleLine) return "";
    return titleLine.replace(/^title\s*:\s*/, "").trim().replace(/^['"]|['"]$/g, "");
  }

  function defaultFileName(content) {
    const title = readFrontMatterTitle(content);
    const slug = title
      .normalize("NFKC")
      .toLowerCase()
      .replace(/[^\p{L}\p{N}]+/gu, "-")
      .replace(/^-+|-+$/g, "");
    return `${slug || `post-${new Date().toISOString().slice(0, 10)}`}.md`;
  }

  function normalizeFilePath(value) {
    const normalized = value.trim().replaceAll("\\", "/").replace(/^\/+/, "");
    const parts = normalized.split("/").filter((part) => part && part !== "." && part !== "..");
    const filename = parts.join("/");
    if (!filename || !/^[^?#]+\.(?:md|mdx)$/i.test(filename)) return null;
    return filename;
  }

  async function openPublish() {
    if (!state.token) {
      await openSettings();
      setStatus("#githubConnectionStatus", "请先验证并保存 GitHub token。", "error");
      return;
    }
    const content = window.mizukiEditor?.getContent?.() || "";
    $("#publish-path").value = defaultFileName(content);
    $("#publishStatus").textContent = "";
    $("#publishVaultStatus").innerHTML = `<strong>🔐 已连接 ${state.record?.owner || "GitHub"}</strong><span>密钥只从当前浏览器的加密存储中读取。</span>`;
    $("#publishModal").classList.remove("hidden");
  }

  function closePublish() {
    $("#publishModal").classList.add("hidden");
  }

  async function publish() {
    const content = window.mizukiEditor?.getContent?.() || "";
    const fileName = normalizeFilePath($("#publish-path").value);
    const message = $("#publish-message").value.trim() || "post: publish article";
    if (!content.trim()) {
      setStatus("#publishStatus", "文章内容为空，先写点内容再发布。", "error");
      return;
    }
    if (!fileName) {
      setStatus("#publishStatus", "文件名必须以 .md 或 .mdx 结尾。", "error");
      return;
    }
    const record = state.record;
    if (!record || !state.token) {
      setStatus("#publishStatus", "本机密钥不可用，请重新验证。", "error");
      return;
    }
    const publishButton = $("#publishConfirm");
    publishButton.disabled = true;
    setStatus("#publishStatus", "正在检查仓库文件…");
    const path = `src/content/posts/${fileName}`;
    const pathUrl = `/repos/${encodeURIComponent(record.owner)}/${encodeURIComponent(record.repo)}/contents/${path.split("/").map(encodeURIComponent).join("/")}`;
    try {
      let existing = null;
      try {
        existing = await githubRequest(`${pathUrl}?ref=${encodeURIComponent(record.branch)}`);
      } catch (error) {
        if (!(error instanceof Error) || !/Not Found/i.test(error.message)) throw error;
      }
      if (existing && !window.confirm(`文件 ${fileName} 已存在，确定覆盖吗？`)) {
        setStatus("#publishStatus", "已取消覆盖。", "");
        return;
      }
      setStatus("#publishStatus", "正在提交 Markdown…");
      const payload = {
        message,
        content: textToBase64(content),
        branch: record.branch,
        ...(existing?.sha ? { sha: existing.sha } : {}),
      };
      const result = await githubRequest(pathUrl, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      setStatus("#publishStatus", `已提交到 ${record.owner}/${record.repo} 的 ${record.branch} 分支，等待站点构建。`, "success");
      if (result?.content?.html_url) {
        const link = document.createElement("a");
        link.href = result.content.html_url;
        link.target = "_blank";
        link.rel = "noopener noreferrer";
        link.textContent = "查看 GitHub 文件";
        link.style.display = "block";
        link.style.marginTop = "6px";
        $("#publishStatus").append(link);
      }
    } catch (error) {
      const messageText = error instanceof Error ? error.message : "发布失败。";
      setStatus("#publishStatus", messageText.includes("Bad credentials") ? "token 无效或已撤销，请重新验证。" : messageText, "error");
    } finally {
      publishButton.disabled = false;
    }
  }

  async function hydrateVault() {
    try {
      if (!window.crypto?.subtle) throw new Error("当前浏览器不支持 Web Crypto。" );
      const record = await readRecord();
      if (!record) return;
      state.token = await decryptRecord(record);
      state.record = record;
    } catch (error) {
      state.token = null;
      state.record = null;
      console.warn("GitHub 密钥暂时不可用：", error instanceof Error ? error.message : error);
    }
  }

  $("#btnGithubSettings").addEventListener("click", () => openSettings().catch((error) => setStatus("#githubConnectionStatus", error.message, "error")));
  $("#githubSettingsClose").addEventListener("click", closeSettings);
  $("#githubSettingsCancel").addEventListener("click", closeSettings);
  $("#githubSettingsModal .modal-overlay").addEventListener("click", closeSettings);
  $("#githubSave").addEventListener("click", () => saveSettings());
  $("#githubForget").addEventListener("click", () => forgetSettings().catch((error) => setStatus("#githubConnectionStatus", error.message, "error")));
  $("#btnPublish").addEventListener("click", () => openPublish().catch((error) => setStatus("#publishStatus", error.message, "error")));
  $("#publishClose").addEventListener("click", closePublish);
  $("#publishCancel").addEventListener("click", closePublish);
  $("#publishModal .modal-overlay").addEventListener("click", closePublish);
  $("#publishConfirm").addEventListener("click", () => publish());
  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") return;
    closeSettings();
    closePublish();
  });

  hydrateVault();
})();

package com.neomelt.mizuki

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.launch

data class EditorState(
    val title: String = "",
    val description: String = "",
    val tags: String = "",
    val path: String = "",
    val body: String = "",
    val owner: String = "",
    val repository: String = "",
    val branch: String = "master",
    val hasToken: Boolean = false,
    val isPublishing: Boolean = false,
    val status: String = "",
    val statusIsError: Boolean = false,
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val tokenStore = SecureTokenStore(application)
    private val githubApi = GitHubApi()

    var state by mutableStateOf(loadState())
        private set

    fun setTitle(value: String) = update { copy(title = value, path = path.ifBlank { defaultPath(value) }) }
    fun setDescription(value: String) = update { copy(description = value) }
    fun setTags(value: String) = update { copy(tags = value) }
    fun setPath(value: String) = update { copy(path = value) }
    fun setBody(value: String) = update { copy(body = value) }

    fun saveSettings(owner: String, repository: String, branch: String, token: String): String? {
        val cleanOwner = owner.trim()
        val cleanRepository = repository.trim()
        val cleanBranch = branch.trim().ifBlank { "master" }
        val cleanToken = token.trim()
        if (cleanOwner.isBlank() || cleanRepository.isBlank()) return "请填写 GitHub 用户名和仓库名。"
        if (cleanToken.isNotBlank()) tokenStore.save(cleanToken)
        if (!tokenStore.hasToken()) return "请首次输入 GitHub Fine-grained token。"
        preferences.edit()
            .putString(KEY_OWNER, cleanOwner)
            .putString(KEY_REPOSITORY, cleanRepository)
            .putString(KEY_BRANCH, cleanBranch)
            .apply()
        state = state.copy(
            owner = cleanOwner,
            repository = cleanRepository,
            branch = cleanBranch,
            hasToken = tokenStore.hasToken(),
            status = "设置已保存。发布时会由 Android Keystore 解密密钥。",
            statusIsError = false,
        )
        return null
    }

    fun clearToken() {
        tokenStore.clear()
        state = state.copy(hasToken = false, status = "已清除本机保存的 GitHub token。", statusIsError = false)
    }

    fun publish() {
        val current = state
        if (current.isPublishing) return
        val token = tokenStore.read()
        if (token.isNullOrBlank() || !current.hasToken) {
            showError("请先在“设置密钥”中保存 GitHub token。")
            return
        }
        if (current.owner.isBlank() || current.repository.isBlank()) {
            showError("请先配置 GitHub 用户名和仓库名。")
            return
        }
        if (current.title.isBlank() || current.body.isBlank()) {
            showError("标题和正文不能为空。")
            return
        }
        val normalizedPath = normalizePath(current.path.ifBlank { defaultPath(current.title) })
        if (normalizedPath == null) {
            showError("文章路径必须以 .md 或 .mdx 结尾，且不能包含 ..。")
            return
        }

        state = current.copy(isPublishing = true, status = "正在检查仓库并发布…", statusIsError = false)
        viewModelScope.launch {
            runCatching {
                githubApi.publish(
                    config = GitHubConfig(current.owner, current.repository, current.branch),
                    token = token,
                    path = "src/content/posts/$normalizedPath",
                    markdown = buildMarkdown(current),
                    message = "post: publish ${current.title.trim()}",
                )
            }.onSuccess { url ->
                state = state.copy(
                    isPublishing = false,
                    status = if (url.isBlank()) "发布成功，等待站点构建。" else "发布成功：$url",
                    statusIsError = false,
                )
            }.onFailure { error ->
                state = state.copy(
                    isPublishing = false,
                    status = error.message ?: "发布失败，请检查网络和 token 权限。",
                    statusIsError = true,
                )
            }
        }
    }

    private fun loadState(): EditorState = EditorState(
        owner = preferences.getString(KEY_OWNER, "") ?: "",
        repository = preferences.getString(KEY_REPOSITORY, "") ?: "",
        branch = preferences.getString(KEY_BRANCH, "master") ?: "master",
        hasToken = tokenStore.hasToken(),
    )

    private fun update(change: EditorState.() -> EditorState) {
        state = state.change()
    }

    private fun showError(message: String) {
        state = state.copy(status = message, statusIsError = true)
    }

    private fun buildMarkdown(current: EditorState): String {
        val tags = current.tags.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(", ") { "\"${yamlEscape(it)}\"" }
        return buildString {
            appendLine("---")
            appendLine("title: \"${yamlEscape(current.title.trim())}\"")
            appendLine("published: ${LocalDate.now()}")
            if (current.description.isNotBlank()) appendLine("description: \"${yamlEscape(current.description.trim())}\"")
            if (tags.isNotBlank()) appendLine("tags: [$tags]")
            appendLine("draft: false")
            appendLine("---")
            appendLine()
            append(current.body.trimEnd())
            appendLine()
        }
    }

    private fun defaultPath(title: String): String {
        val slug = title.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
            .trim('-')
            .ifBlank { "post-${LocalDate.now()}" }
        return "$slug.md"
    }

    private fun normalizePath(value: String): String? {
        val normalized = value.trim().replace('\\', '/').trim('/')
        if (normalized.isBlank() || normalized.split('/').any { it == ".." || it.isBlank() }) return null
        if (!normalized.matches(Regex("^[^?#]+\\.(?i:md|mdx)$"))) return null
        return normalized
    }

    private fun yamlEscape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")

    private companion object {
        const val PREFERENCES = "mizuki-editor-settings"
        const val KEY_OWNER = "github-owner"
        const val KEY_REPOSITORY = "github-repository"
        const val KEY_BRANCH = "github-branch"
    }
}

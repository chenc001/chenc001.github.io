import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const source = path.join(root, "docs", "editor");
const target = path.join(root, "public", "editor");
const marked = path.join(root, "node_modules", "marked", "lib", "marked.umd.js");

fs.mkdirSync(target, { recursive: true });
for (const entry of fs.readdirSync(source, { withFileTypes: true })) {
	if (entry.name === "vendor") continue;
	const from = path.join(source, entry.name);
	const to = path.join(target, entry.name);
	if (entry.isDirectory()) {
		fs.cpSync(from, to, { recursive: true, force: true });
	} else {
		fs.copyFileSync(from, to);
	}
}

fs.mkdirSync(path.join(target, "vendor"), { recursive: true });
fs.copyFileSync(marked, path.join(target, "vendor", "marked.umd.js"));
console.log("Editor assets synced to public/editor");

export type AIToolCategory =
	| "chat"
	| "coding"
	| "image"
	| "audio"
	| "video"
	| "writing"
	| "search"
	| "other";

export type AIToolFrequency =
	| "daily"
	| "weekly"
	| "occasional"
	| "experimental";

export type LocaleString = Partial<
	Record<"en" | "zh_CN" | "zh_TW" | "ja", string>
>;

export function getLocaleString(value: LocaleString, lang: string): string {
	return value[lang as keyof LocaleString] ?? value.en ?? "";
}

export interface AITool {
	id: string;
	name: string;
	description: LocaleString;
	icon: string;
	category: AIToolCategory;
	frequency: AIToolFrequency;
	url?: string;
	usage?: LocaleString;
	tags?: string[];
	color?: string;
}

// 在这里维护常用的 AI 工具
export const aiToolsData: AITool[] = [
	{
		id: "example-chat",
		name: "对话助手",
		description: {
			en: "A conversational AI assistant for writing and reasoning.",
			zh_CN: "用于写作与推理的对话式 AI 助手。",
		},
		icon: "material-symbols:smart-toy",
		category: "chat",
		frequency: "daily",
		usage: {
			en: "Daily: writing, brainstorming",
			zh_CN: "每天：写作、思路梳理",
		},
		tags: ["对话"],
		color: "#C97758",
	},
	{
		id: "example-coding",
		name: "编程助手",
		description: {
			en: "An AI-powered code completion and review tool.",
			zh_CN: "AI 驱动的代码补全与审查工具。",
		},
		icon: "material-symbols:code",
		category: "coding",
		frequency: "weekly",
		usage: {
			en: "Weekly: code review, refactoring",
			zh_CN: "每周：代码审查、重构",
		},
		tags: ["编程"],
		color: "#10A37F",
	},
	{
		id: "example-image",
		name: "图像助手",
		description: {
			en: "An AI image generation tool for creating illustrations.",
			zh_CN: "用于生成插图的 AI 图像工具。",
		},
		icon: "material-symbols:image",
		category: "image",
		frequency: "occasional",
		tags: ["图像"],
		color: "#1A73E8",
	},
];

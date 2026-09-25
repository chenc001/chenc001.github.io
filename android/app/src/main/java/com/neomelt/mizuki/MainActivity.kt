package com.neomelt.mizuki

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

private val MizukiBackground = Color(0xFF171710)
private val MizukiSurface = Color(0xFF201F18)
private val MizukiElevated = Color(0xFF2A2920)
private val MizukiBorder = Color(0xFF4D4B40)
private val MizukiText = Color(0xFFE8E6D8)
private val MizukiMuted = Color(0xFFA7A597)
private val MizukiAccent = Color(0xFFFFDD00)
private val MizukiAccentDim = Color(0xFF4B4300)
private val MizukiBlue = Color(0xFF8DD9FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MizukiTheme { EditorApp() } }
    }
}

@Composable
private fun MizukiTheme(content: @Composable () -> Unit) {
    val colors = androidx.compose.material3.darkColorScheme(
        primary = MizukiAccent,
        onPrimary = Color(0xFF29260A),
        primaryContainer = MizukiAccentDim,
        onPrimaryContainer = Color(0xFFFFF3A3),
        secondary = MizukiBlue,
        onSecondary = Color(0xFF06202B),
        background = MizukiBackground,
        surface = MizukiSurface,
        surfaceVariant = MizukiElevated,
        onSurface = MizukiText,
        onSurfaceVariant = MizukiMuted,
        outline = MizukiBorder,
        error = Color(0xFFFF8A80),
    )
    val typography = androidx.compose.material3.Typography(
        headlineSmall = TextStyle(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Medium),
    )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorApp(viewModel: EditorViewModel = viewModel()) {
    val state = viewModel.state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var templateOpen by rememberSaveable { mutableStateOf(false) }
    var updateDialogOpen by rememberSaveable { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var updateProgress by remember { mutableStateOf<Int?>(null) }
    var updateError by remember { mutableStateOf<String?>(null) }
    var viewMode by rememberSaveable { mutableStateOf("edit") }
    var bodyField by remember { mutableStateOf(TextFieldValue(state.body)) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        availableUpdate = AppUpdateService.checkForUpdate()
    }
    androidx.compose.runtime.LaunchedEffect(state.body) {
        if (bodyField.text != state.body) bodyField = TextFieldValue(state.body)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("✦", color = MizukiAccent, fontSize = 25.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Mizuki Editor", style = MaterialTheme.typography.titleMedium)
                            Text("手机端写作工作台 · ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MizukiMuted)
                        }
                    }
                },
                actions = {
                    if (availableUpdate != null) {
                        TextButton(onClick = { updateDialogOpen = true }) {
                            Text("更新", color = MizukiAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                    TokenBadge(hasToken = state.hasToken)
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { settingsOpen = true }) {
                        Text("⚙", fontSize = 20.sp, color = MizukiText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MizukiSurface.copy(alpha = 0.92f),
                    titleContentColor = MizukiText,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            Text("WRITE", style = MaterialTheme.typography.labelSmall, color = MizukiAccent)
            Text("写一篇新文章", style = MaterialTheme.typography.headlineSmall)
            Text(
                "文章会整理成 Front Matter + Markdown，并提交到你的博客仓库。",
                style = MaterialTheme.typography.bodyMedium,
                color = MizukiMuted,
            )
            availableUpdate?.let { update ->
                UpdateBanner(update = update, onClick = { updateDialogOpen = true })
            }

            EditorCard(label = "FRONT MATTER", hint = "文章元数据") {
                MizukiTextField(
                    value = state.title,
                    onValueChange = viewModel::setTitle,
                    label = "文章标题",
                    placeholder = "给文章起一个标题",
                )
                MizukiTextField(
                    value = state.description,
                    onValueChange = viewModel::setDescription,
                    label = "描述",
                    placeholder = "一句话介绍文章（可选）",
                    minLines = 2,
                )
                MizukiTextField(
                    value = state.tags,
                    onValueChange = viewModel::setTags,
                    label = "标签",
                    placeholder = "用逗号分隔，例如：随笔, 技术",
                )
                MizukiTextField(
                    value = state.path,
                    onValueChange = viewModel::setPath,
                    label = "文件名",
                    placeholder = "article-slug.md",
                    supportingText = "保存到 src/content/posts/",
                )
            }

            EditorCard(label = "MARKDOWN", hint = "正文") {
                ViewModeTabs(mode = viewMode, onModeChange = { viewMode = it })
                if (viewMode != "preview") {
                    MarkdownToolbar(
                        onAction = { action ->
                            bodyField = applyMarkdownAction(bodyField, action)
                            viewModel.setBody(bodyField.text)
                        },
                        onTemplate = { templateOpen = true },
                    )
                }
                when (viewMode) {
                    "preview" -> MarkdownPreview(state.body)
                    "split" -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MarkdownEditor(
                            value = bodyField,
                            onValueChange = {
                                bodyField = it
                                viewModel.setBody(it.text)
                            },
                            compact = true,
                        )
                        MarkdownPreview(state.body)
                    }
                    else -> MarkdownEditor(
                        value = bodyField,
                        onValueChange = {
                            bodyField = it
                            viewModel.setBody(it.text)
                        },
                    )
                }
            }

            PublishCard(
                state = state,
                onSettings = { settingsOpen = true },
                onPublish = viewModel::publish,
            )
            Spacer(Modifier.height(22.dp))
        }
    }

    if (settingsOpen) {
        SettingsDialog(
            state = state,
            onDismiss = { settingsOpen = false },
            onSave = { owner, repository, branch, token ->
                val error = viewModel.saveSettings(owner, repository, branch, token)
                if (error == null) settingsOpen = false
                error
            },
            onClear = {
                viewModel.clearToken()
                settingsOpen = false
            },
        )
    }

    if (templateOpen) {
        TemplatePickerDialog(
            onDismiss = { templateOpen = false },
            onSelect = { template ->
                bodyField = TextFieldValue(template)
                viewModel.setBody(template)
                templateOpen = false
            },
        )
    }

    if (updateDialogOpen) {
        availableUpdate?.let { update ->
            UpdateDialog(
                update = update,
                progress = updateProgress,
                error = updateError,
                onDismiss = {
                    if (updateProgress == null) updateDialogOpen = false
                },
                onInstall = {
                    scope.launch {
                        updateError = null
                        if (!AppUpdateService.canInstallPackages(context)) {
                            AppUpdateService.openInstallPermission(context)
                            updateError = "请允许本应用安装未知来源应用，然后返回这里再次点击更新。"
                            return@launch
                        }
                        try {
                            updateProgress = 0
                            val uri = AppUpdateService.download(context, update) { value -> updateProgress = value }
                            AppUpdateService.install(context, uri)
                        } catch (error: Throwable) {
                            updateProgress = null
                            updateError = error.message ?: "下载更新失败，请稍后重试。"
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun UpdateBanner(update: AppUpdate, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MizukiAccentDim.copy(alpha = 0.72f)),
        border = BorderStroke(1.dp, MizukiAccent.copy(alpha = 0.55f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("发现新版本 ${update.version}", color = MizukiAccent, fontWeight = FontWeight.Bold)
                Text("点击下载并安装更新", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFF3A3))
            }
            Text("→", color = MizukiAccent, fontSize = 22.sp)
        }
    }
}

@Composable
private fun UpdateDialog(
    update: AppUpdate,
    progress: Int?,
    error: String?,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MizukiSurface,
        titleContentColor = MizukiText,
        textContentColor = MizukiText,
        shape = MaterialTheme.shapes.large,
        title = { Text("发现新版本 ${update.version}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("更新包：${update.assetName}", color = MizukiMuted)
                Text("应用会从 GitHub Releases 下载更新，并调用系统安装器完成升级。", color = MizukiText)
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MizukiAccent,
                        trackColor = MizukiBorder,
                    )
                    Text("正在下载 $progress%", color = MizukiBlue, style = MaterialTheme.typography.bodySmall)
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                onClick = onInstall,
                enabled = progress == null,
                colors = ButtonDefaults.buttonColors(containerColor = MizukiAccent, contentColor = Color(0xFF29260A)),
            ) { Text(if (progress == null) "下载并更新" else "下载中…", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = progress == null) { Text("稍后", color = MizukiMuted) } },
    )
}

@Composable
private fun TokenBadge(hasToken: Boolean) {
    val text = if (hasToken) "已连接" else "未连接"
    val color = if (hasToken) Color(0xFFB8F0C0) else MizukiMuted
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), MaterialTheme.shapes.small)
            .border(1.dp, color.copy(alpha = 0.35f), MaterialTheme.shapes.small)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("●", color = color, fontSize = 9.sp)
        Text(text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EditorCard(label: String, hint: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MizukiSurface.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, MizukiBorder.copy(alpha = 0.75f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MizukiAccent)
                Text("·", color = MizukiBorder)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MizukiMuted)
            }
            content()
        }
    }
}

@Composable
private fun MizukiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    supportingText: String? = null,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = MizukiMuted.copy(alpha = 0.72f)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        singleLine = minLines == 1,
        supportingText = supportingText?.let { { Text(it, color = MizukiMuted) } },
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MizukiText,
            unfocusedTextColor = MizukiText,
            focusedBorderColor = MizukiAccent,
            unfocusedBorderColor = MizukiBorder,
            focusedLabelColor = MizukiAccent,
            unfocusedLabelColor = MizukiMuted,
            cursorColor = MizukiAccent,
            focusedContainerColor = MizukiElevated.copy(alpha = 0.70f),
            unfocusedContainerColor = MizukiElevated.copy(alpha = 0.45f),
        ),
    )
}

@Composable
private fun MarkdownEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    compact: Boolean = false,
) {
    val textStyle = TextStyle(
        color = MizukiText,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 220.dp else 320.dp)
            .background(MizukiBackground.copy(alpha = 0.82f), MaterialTheme.shapes.medium)
            .border(1.dp, MizukiBorder, MaterialTheme.shapes.medium)
            .padding(14.dp),
        textStyle = textStyle,
        cursorBrush = Brush.verticalGradient(listOf(MizukiAccent, MizukiAccent)),
        decorationBox = { innerTextField ->
            Box {
                if (value.text.isBlank()) {
                    Text(
                        "# 在这里写 Markdown\n\n支持标题、列表、代码块和链接…",
                        color = MizukiMuted.copy(alpha = 0.72f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                    )
                }
                innerTextField()
            }
        },
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Markdown", style = MaterialTheme.typography.labelSmall, color = MizukiMuted)
        Text("${value.text.length} 字符", style = MaterialTheme.typography.labelSmall, color = MizukiMuted)
    }
}

private enum class MarkdownAction {
    HEADING_1,
    HEADING_2,
    HEADING_3,
    BOLD,
    ITALIC,
    STRIKE,
    UNORDERED_LIST,
    ORDERED_LIST,
    TASK_LIST,
    QUOTE,
    INLINE_CODE,
    CODE_BLOCK,
    LINK,
    IMAGE,
    TABLE,
    HORIZONTAL_RULE,
    ADMONITION,
    MERMAID,
    PLANTUML,
    MATH,
    DETAILS,
    VIDEO,
    HTML,
    GITHUB_CARD,
    SPOILER,
    EXCERPT,
    YOUTUBE,
    BILIBILI,
}

@Composable
private fun ViewModeTabs(mode: String, onModeChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf("edit" to "编辑", "split" to "分屏", "preview" to "预览").forEach { (value, label) ->
            val active = mode == value
            TextButton(
                onClick = { onModeChange(value) },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    containerColor = if (active) MizukiAccentDim else Color.Transparent,
                    contentColor = if (active) MizukiAccent else MizukiMuted,
                ),
            ) { Text(label) }
        }
    }
}

@Composable
private fun MarkdownToolbar(
    onAction: (MarkdownAction) -> Unit,
    onTemplate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(
            MarkdownAction.HEADING_1 to "H1",
            MarkdownAction.HEADING_2 to "H2",
            MarkdownAction.HEADING_3 to "H3",
            MarkdownAction.BOLD to "B",
            MarkdownAction.ITALIC to "I",
            MarkdownAction.STRIKE to "S",
            MarkdownAction.UNORDERED_LIST to "•",
            MarkdownAction.ORDERED_LIST to "1.",
            MarkdownAction.TASK_LIST to "☑",
            MarkdownAction.QUOTE to "❝",
            MarkdownAction.INLINE_CODE to "</>",
            MarkdownAction.CODE_BLOCK to "{ }",
            MarkdownAction.LINK to "🔗",
            MarkdownAction.IMAGE to "🖼",
            MarkdownAction.TABLE to "▦",
            MarkdownAction.HORIZONTAL_RULE to "―",
            MarkdownAction.ADMONITION to "提示",
            MarkdownAction.MERMAID to "图表",
            MarkdownAction.PLANTUML to "UML",
            MarkdownAction.MATH to "公式",
            MarkdownAction.DETAILS to "折叠",
            MarkdownAction.VIDEO to "视频",
            MarkdownAction.HTML to "HTML",
            MarkdownAction.GITHUB_CARD to "GitHub",
            MarkdownAction.SPOILER to "隐藏",
            MarkdownAction.EXCERPT to "摘要",
            MarkdownAction.YOUTUBE to "YouTube",
            MarkdownAction.BILIBILI to "B站",
        ).forEach { (action, label) ->
            TextButton(
                onClick = { onAction(action) },
                modifier = Modifier.heightIn(min = 34.dp),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = if (action.ordinal >= MarkdownAction.ADMONITION.ordinal) MizukiBlue else MizukiText,
                ),
            ) {
                Text(label, fontSize = if (label.length > 2) 12.sp else 14.sp)
            }
        }
        TextButton(
            onClick = onTemplate,
            modifier = Modifier.heightIn(min = 34.dp),
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MizukiAccent),
        ) { Text("模板", fontSize = 12.sp) }
    }
}

private fun applyMarkdownAction(field: TextFieldValue, action: MarkdownAction): TextFieldValue {
    val text = field.text
    val start = minOf(field.selection.start, field.selection.end)
    val end = maxOf(field.selection.start, field.selection.end)
    val selected = text.substring(start, end)

    fun wrapped(before: String, after: String, placeholder: String): TextFieldValue {
        val body = selected.ifBlank { placeholder }
        val replacement = "$before$body$after"
        val next = text.replaceRange(start, end, replacement)
        val selection = if (selected.isBlank()) {
            TextRange(start + before.length, start + before.length + body.length)
        } else {
            TextRange(start + replacement.length)
        }
        return TextFieldValue(next, selection)
    }

    fun prefixed(prefix: String): TextFieldValue {
        val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', end).let { if (it < 0) text.length else it }
        val lines = text.substring(lineStart, lineEnd).lines()
        val replacement = lines.joinToString("\n") { line -> if (line.startsWith(prefix)) line else prefix + line }
        val next = text.replaceRange(lineStart, lineEnd, replacement)
        return TextFieldValue(next, TextRange(lineStart + replacement.length))
    }

    fun inserted(value: String): TextFieldValue {
        val next = text.replaceRange(start, end, value)
        return TextFieldValue(next, TextRange(start + value.length))
    }

    return when (action) {
        MarkdownAction.HEADING_1 -> prefixed("# ")
        MarkdownAction.HEADING_2 -> prefixed("## ")
        MarkdownAction.HEADING_3 -> prefixed("### ")
        MarkdownAction.BOLD -> wrapped("**", "**", "粗体")
        MarkdownAction.ITALIC -> wrapped("*", "*", "斜体")
        MarkdownAction.STRIKE -> wrapped("~~", "~~", "删除线")
        MarkdownAction.UNORDERED_LIST -> prefixed("- ")
        MarkdownAction.ORDERED_LIST -> prefixed("1. ")
        MarkdownAction.TASK_LIST -> prefixed("- [ ] ")
        MarkdownAction.QUOTE -> prefixed("> ")
        MarkdownAction.INLINE_CODE -> wrapped("`", "`", "代码")
        MarkdownAction.CODE_BLOCK -> wrapped("```\n", "\n```", "在这里写代码")
        MarkdownAction.LINK -> wrapped("[", "](https://)", "链接文字")
        MarkdownAction.IMAGE -> wrapped("![", "](https://)", "图片描述")
        MarkdownAction.TABLE -> inserted("| 项目 | 内容 |\n| --- | --- |\n| 示例 | 填写内容 |\n")
        MarkdownAction.HORIZONTAL_RULE -> inserted("\n---\n")
        MarkdownAction.ADMONITION -> inserted("\n:::note\n这里写提示内容\n:::\n")
        MarkdownAction.MERMAID -> inserted("\n```mermaid\nflowchart TD\n    A[开始] --> B[下一步]\n```\n")
        MarkdownAction.PLANTUML -> inserted("\n```plantuml\n@startuml\nAlice -> Bob: Hello\n@enduml\n```\n")
        MarkdownAction.MATH -> inserted("\n$$\nE = mc^2\n$$\n")
        MarkdownAction.DETAILS -> inserted("\n<details>\n<summary>点击展开</summary>\n\n这里写折叠内容\n\n</details>\n")
        MarkdownAction.VIDEO -> inserted("\n@[video](https://example.com/video.mp4)\n")
        MarkdownAction.HTML -> inserted("\n<div class=\"custom\">\n  在这里写 HTML 内容\n</div>\n")
        MarkdownAction.GITHUB_CARD -> inserted("\n:::github{repo=\"用户名/仓库名\"}\n\n")
        MarkdownAction.SPOILER -> wrapped(":spoiler[", "]", "隐藏的内容")
        MarkdownAction.EXCERPT -> inserted("\n<!--more-->\n")
        MarkdownAction.YOUTUBE -> inserted("\n<iframe width=\"100%\" height=\"315\" src=\"https://www.youtube.com/embed/视频ID\" title=\"YouTube\" frameborder=\"0\" allowfullscreen></iframe>\n")
        MarkdownAction.BILIBILI -> inserted("\n<iframe width=\"100%\" height=\"315\" src=\"https://player.bilibili.com/player.html?bvid=视频BV号&p=1\" scrolling=\"no\" frameborder=\"no\" allowfullscreen=\"true\"></iframe>\n")
    }
}

@Composable
private fun MarkdownPreview(markdown: String) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .border(1.dp, MizukiBorder, MaterialTheme.shapes.medium),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.rgb(23, 23, 16))
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                settings.allowFileAccessFromFileURLs = false
                settings.allowUniversalAccessFromFileURLs = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = true

                    override fun onPageFinished(view: WebView?, url: String?) {
                        val current = view?.tag as? String ?: return
                        val encoded = JSONObject.quote(current)
                        view.evaluateJavascript("window.renderMarkdown ? window.renderMarkdown($encoded) : (window.pendingMarkdown=$encoded);", null)
                    }
                }
                tag = markdown
                loadUrl("file:///android_asset/preview/index.html")
            }
        },
        update = { webView ->
            webView.tag = markdown
            val encoded = JSONObject.quote(markdown)
            webView.post {
                webView.evaluateJavascript(
                    "window.renderMarkdown ? window.renderMarkdown($encoded) : (window.pendingMarkdown=$encoded);",
                    null,
                )
            }
        },
    )
}

@Composable
private fun TemplatePickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val templates = listOf(
        "技术教程" to "# 技术教程\n\n## 问题\n\n## 方案\n\n## 实现\n\n## 总结\n",
        "生活随笔" to "# 今天的记录\n\n今天想记录的事情：\n\n## 片段\n\n## 感受\n",
        "项目记录" to "# 项目进展记录\n\n## 本次完成\n\n- [ ] 待办事项\n\n## 遇到的问题\n\n## 下一步\n",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MizukiSurface,
        titleContentColor = MizukiText,
        textContentColor = MizukiText,
        title = { Text("选择文章模板") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                templates.forEach { (name, content) ->
                    OutlinedButton(
                        onClick = { onSelect(content) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MizukiBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MizukiText),
                    ) { Text(name) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消", color = MizukiMuted) } },
    )
}

@Composable
private fun PublishCard(
    state: EditorState,
    onSettings: () -> Unit,
    onPublish: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MizukiElevated.copy(alpha = 0.74f)),
        border = BorderStroke(1.dp, if (state.statusIsError) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MizukiAccent.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("发布到 GitHub", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.owner.isBlank()) "还没有配置目标仓库" else "${state.owner}/${state.repository} · ${state.branch}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MizukiMuted,
                    )
                }
                Text("↗", color = MizukiAccent, fontSize = 24.sp)
            }
            if (state.status.isNotBlank()) {
                Text(
                    text = state.status,
                    color = if (state.statusIsError) MaterialTheme.colorScheme.error else MizukiBlue,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider(color = MizukiBorder.copy(alpha = 0.7f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MizukiBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MizukiText),
                ) { Text("设置") }
                Button(
                    onClick = onPublish,
                    enabled = !state.isPublishing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MizukiAccent,
                        contentColor = Color(0xFF29260A),
                        disabledContainerColor = MizukiAccent.copy(alpha = 0.45f),
                    ),
                ) { Text(if (state.isPublishing) "发布中…" else "发布文章", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    state: EditorState,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> String?,
    onClear: () -> Unit,
) {
    var owner by remember(state.owner) { mutableStateOf(state.owner) }
    var repository by remember(state.repository) { mutableStateOf(state.repository) }
    var branch by remember(state.branch) { mutableStateOf(state.branch) }
    var token by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MizukiSurface,
        titleContentColor = MizukiText,
        textContentColor = MizukiText,
        shape = MaterialTheme.shapes.large,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("GitHub 发布设置")
                Text("SECURITY VAULT", style = MaterialTheme.typography.labelSmall, color = MizukiAccent)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Token 只需首次输入，保存后由 Android Keystore 加密保存。", color = MizukiMuted)
                MizukiTextField(owner, { owner = it }, "用户名或组织", "例如 chenc001")
                MizukiTextField(repository, { repository = it }, "仓库名", "例如 chenc001.github.io")
                MizukiTextField(branch, { branch = it }, "分支", "master")
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(if (state.hasToken) "Token（留空则保留）" else "Fine-grained token") },
                    placeholder = { Text("只保存在本机") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MizukiText,
                        unfocusedTextColor = MizukiText,
                        focusedBorderColor = MizukiAccent,
                        unfocusedBorderColor = MizukiBorder,
                        focusedLabelColor = MizukiAccent,
                        unfocusedLabelColor = MizukiMuted,
                        cursorColor = MizukiAccent,
                        focusedContainerColor = MizukiElevated,
                        unfocusedContainerColor = MizukiElevated.copy(alpha = 0.65f),
                    ),
                )
                if (state.hasToken) Text("● 当前设备已有加密保存的 token。", color = Color(0xFFB8F0C0), style = MaterialTheme.typography.bodySmall)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                onClick = { error = onSave(owner, repository, branch, token) },
                colors = ButtonDefaults.buttonColors(containerColor = MizukiAccent, contentColor = Color(0xFF29260A)),
            ) { Text("保存设置", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (state.hasToken) TextButton(onClick = onClear) { Text("清除密钥", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("取消", color = MizukiMuted) }
            }
        },
    )
}

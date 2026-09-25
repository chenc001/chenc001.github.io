package com.neomelt.mizuki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

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
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

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
                            Text("手机端写作工作台", style = MaterialTheme.typography.labelSmall, color = MizukiMuted)
                        }
                    }
                },
                actions = {
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
                MarkdownEditor(
                    value = state.body,
                    onValueChange = viewModel::setBody,
                )
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
private fun MarkdownEditor(value: String, onValueChange: (String) -> Unit) {
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
            .height(320.dp)
            .background(MizukiBackground.copy(alpha = 0.82f), MaterialTheme.shapes.medium)
            .border(1.dp, MizukiBorder, MaterialTheme.shapes.medium)
            .padding(14.dp),
        textStyle = textStyle,
        cursorBrush = Brush.verticalGradient(listOf(MizukiAccent, MizukiAccent)),
        decorationBox = { innerTextField ->
            Box {
                if (value.isBlank()) {
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
        Text("${value.length} 字符", style = MaterialTheme.typography.labelSmall, color = MizukiMuted)
    }
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

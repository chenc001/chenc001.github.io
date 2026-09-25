package com.neomelt.mizuki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MizukiTheme { EditorApp() } }
    }
}

@Composable
private fun MizukiTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorApp(viewModel: EditorViewModel = viewModel()) {
    val state = viewModel.state
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("辰萃写作") },
                actions = {
                    TextButton(onClick = { settingsOpen = true }) { Text("设置密钥") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text("文章标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                label = { Text("描述（可选）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            OutlinedTextField(
                value = state.tags,
                onValueChange = viewModel::setTags,
                label = { Text("标签，用逗号分隔（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.path,
                onValueChange = viewModel::setPath,
                label = { Text("文件名") },
                supportingText = { Text("将写入 src/content/posts/") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.body,
                onValueChange = viewModel::setBody,
                label = { Text("Markdown 正文") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                minLines = 12,
            )
            HorizontalDivider()
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (state.hasToken) "🔐 GitHub 密钥已保存" else "尚未配置 GitHub 密钥",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (state.owner.isBlank()) "请打开设置，填写目标仓库。" else "目标：${state.owner}/${state.repository}（${state.branch}）",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { settingsOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("设置")
                }
                Button(
                    onClick = viewModel::publish,
                    enabled = !state.isPublishing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isPublishing) "发布中…" else "发布到 GitHub")
                }
            }
            if (state.status.isNotBlank()) {
                Text(
                    text = state.status,
                    color = if (state.statusIsError) MaterialTheme.colorScheme.error else Color.Unspecified,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(24.dp))
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
        title = { Text("GitHub 发布设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Token 只需首次输入，保存后由 Android Keystore 加密保存。")
                OutlinedTextField(owner, { owner = it }, label = { Text("用户名或组织") }, singleLine = true)
                OutlinedTextField(repository, { repository = it }, label = { Text("仓库名") }, singleLine = true)
                OutlinedTextField(branch, { branch = it }, label = { Text("分支") }, singleLine = true)
                OutlinedTextField(
                    token,
                    { token = it },
                    label = { Text(if (state.hasToken) "Token（留空则保留现有密钥）" else "Fine-grained token") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                if (state.hasToken) Text("当前设备已有加密保存的 token。", style = MaterialTheme.typography.bodySmall)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = { error = onSave(owner, repository, branch, token) }) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (state.hasToken) TextButton(onClick = onClear) { Text("清除密钥") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

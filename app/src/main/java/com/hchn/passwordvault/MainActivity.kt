package com.hchn.passwordvault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hchn.passwordvault.model.VaultEntry
import com.hchn.passwordvault.ui.VaultUiState
import com.hchn.passwordvault.ui.VaultViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: VaultViewModel by viewModels()
    private var backgroundAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            PasswordVaultTheme {
                PasswordVaultApp(state = state, viewModel = viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundAt > 0 && System.currentTimeMillis() - backgroundAt >= AUTO_LOCK_MS) {
            viewModel.lock()
        }
    }

    private companion object {
        const val AUTO_LOCK_MS = 120_000L
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF3156D3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE3FF),
    surface = Color(0xFFF9FAFF),
    surfaceContainer = Color.White,
    background = Color(0xFFF5F7FC),
    error = Color(0xFFBA1A1A)
)

@Composable
private fun PasswordVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}

@Composable
private fun PasswordVaultApp(state: VaultUiState, viewModel: VaultViewModel) {
    if (state.unlocked) {
        VaultScreen(
            state = state,
            onQueryChange = viewModel::setQuery,
            onSave = viewModel::saveEntry,
            onDelete = viewModel::deleteEntry,
            onLock = viewModel::lock
        )
    } else {
        UnlockScreen(
            configured = state.configured,
            error = state.error,
            onUnlock = viewModel::unlock,
            onCreate = viewModel::createVault,
            onClearError = viewModel::clearError
        )
    }
}

@Composable
private fun UnlockScreen(
    configured: Boolean,
    error: String?,
    onUnlock: (String) -> Unit,
    onCreate: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(password, confirmation) { if (error != null) onClearError() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (configured) Icons.Default.Lock else Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.height(22.dp))
            Text("密码本", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (configured) "输入主密码解锁你的保险库" else "创建一个只属于你的加密保险库",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 26.dp)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (configured) "主密码" else "设置主密码（至少 8 位）") },
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (!configured) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text("再次输入主密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (configured) onUnlock(password) else onCreate(password, confirmation)
                },
                enabled = password.isNotBlank() && (configured || confirmation.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(if (configured) Icons.Default.LockOpen else Icons.Default.Lock, null)
                Text(if (configured) "解锁" else "创建密码库", modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                "主密码不会上传，也无法找回。所有数据仅加密保存在本机。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultScreen(
    state: VaultUiState,
    onQueryChange: (String) -> Unit,
    onSave: (VaultEntry) -> Unit,
    onDelete: (VaultEntry) -> Unit,
    onLock: () -> Unit
) {
    var editing by remember { mutableStateOf<VaultEntry?>(null) }
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("我的密码库", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onLock) { Icon(Icons.Default.Lock, "锁定") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Default.Add, "添加密码")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("搜索名称、账号或网站") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, "清除") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("全部密码", fontWeight = FontWeight.SemiBold)
                Text("${state.filteredEntries.size} 项", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.filteredEntries.isEmpty()) {
                EmptyVault(hasQuery = state.query.isNotBlank(), onAdd = { adding = true })
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.filteredEntries, key = { it.id }) { entry ->
                        EntryCard(entry = entry, onEdit = { editing = entry }, onDelete = { onDelete(entry) })
                    }
                    item { Spacer(Modifier.height(92.dp)) }
                }
            }
        }
    }

    if (adding || editing != null) {
        EntryEditor(
            entry = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { onSave(it); adding = false; editing = null }
        )
    }
}

@Composable
private fun EmptyVault(hasQuery: Boolean, onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Key, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            if (hasQuery) "没有找到匹配的密码" else "密码库还是空的",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp)
        )
        if (!hasQuery) {
            Text("添加第一条账号密码，数据只保存在本机", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onAdd, modifier = Modifier.padding(top = 18.dp)) {
                Icon(Icons.Default.Add, null)
                Text("添加密码", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun EntryCard(entry: VaultEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(entry.title.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(entry.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text(entry.username.ifBlank { "未填写账号" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "编辑") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.DeleteOutline, "删除") }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (revealed) entry.password else "•".repeat(entry.password.length.coerceIn(8, 16)),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(onClick = { revealed = !revealed }) {
                    Icon(if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility, "显示或隐藏密码")
                }
                IconButton(onClick = { copyTemporarily(context, entry.password) }) {
                    Icon(Icons.Default.ContentCopy, "复制密码")
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除“${entry.title}”？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = { TextButton(onClick = { onDelete(); confirmDelete = false }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun EntryEditor(entry: VaultEntry?, onDismiss: () -> Unit, onSave: (VaultEntry) -> Unit) {
    var title by remember { mutableStateOf(entry?.title.orEmpty()) }
    var username by remember { mutableStateOf(entry?.username.orEmpty()) }
    var password by remember { mutableStateOf(entry?.password.orEmpty()) }
    var website by remember { mutableStateOf(entry?.website.orEmpty()) }
    var note by remember { mutableStateOf(entry?.note.orEmpty()) }
    var visible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "添加密码" else "编辑密码") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(title, { title = it }, label = { Text("名称 *") }, singleLine = true) }
                item { OutlinedTextField(username, { username = it }, label = { Text("账号 / 邮箱") }, singleLine = true) }
                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码 *") },
                        singleLine = true,
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { password = VaultViewModel.generatePassword() }) { Icon(Icons.Default.Refresh, "生成密码") }
                                IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) }
                            }
                        }
                    )
                }
                item { OutlinedTextField(website, { website = it }, label = { Text("网站") }, singleLine = true) }
                item { OutlinedTextField(note, { note = it }, label = { Text("备注") }, minLines = 2, maxLines = 4) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        VaultEntry(
                            id = entry?.id ?: java.util.UUID.randomUUID().toString(),
                            title = title.trim(),
                            username = username.trim(),
                            password = password,
                            website = website.trim(),
                            note = note.trim()
                        )
                    )
                },
                enabled = title.isNotBlank() && password.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun copyTemporarily(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("密码", value))
    Handler(Looper.getMainLooper()).postDelayed({
        val current = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
        if (current == value) clipboard.clearPrimaryClip()
    }, 30_000L)
}

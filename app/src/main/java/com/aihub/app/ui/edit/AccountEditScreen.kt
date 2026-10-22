package com.aihub.app.ui.edit

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aihub.app.data.model.BillingCycle
import com.aihub.app.data.model.PlanSource
import com.aihub.app.data.model.Platform
import com.aihub.app.data.model.QuotaSource
import com.aihub.app.data.model.SubscriptionStatus
import com.aihub.app.ui.AppViewModelProvider
import com.aihub.app.ui.components.BalanceBlock
import com.aihub.app.ui.components.PlatformAvatar
import com.aihub.app.ui.components.openOfficialPage
import com.aihub.app.util.formatDate
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditScreen(
    onDone: () -> Unit,
    viewModel: AccountEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "添加账号" else "编辑账号") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除账号")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!viewModel.loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            AccountForm(
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
                onSave = { viewModel.save(onDone) },
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这个账号？") },
            text = { Text("账号记录和加密备注都会从本机删除，无法恢复。不会影响你在官方平台上的账号。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDone)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountForm(viewModel: AccountEditViewModel, modifier: Modifier, onSave: () -> Unit) {
    val form = viewModel.form
    val showErrors = viewModel.showErrors
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Section("平台")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Platform.entries.forEach { p ->
                FilterChip(
                    selected = form.platform == p,
                    onClick = { viewModel.selectPlatform(p) },
                    label = { Text(p.displayName) },
                    leadingIcon = { PlatformAvatar(p, size = 18.dp) },
                )
            }
        }

        Section("账号")
        OutlinedTextField(
            value = form.label,
            onValueChange = { v -> viewModel.update { it.copy(label = v) } },
            label = { Text("名称（如：工作号）") },
            singleLine = true,
            isError = showErrors && form.labelError != null,
            supportingText = form.labelError?.takeIf { showErrors }?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth().testTag("field_label"),
        )
        OutlinedTextField(
            value = form.email,
            onValueChange = { v -> viewModel.update { it.copy(email = v) } },
            label = { Text("登录邮箱（可选）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().testTag("field_email"),
        )

        Section("会员套餐")
        viewModel.stored?.takeIf { it.planSource == PlanSource.RECEIPT }?.let { a ->
            Text(
                "以下套餐信息来自收据识别" + (a.planImportedAt?.let { "（${formatDate(it)}）" } ?: "") +
                    "。修改后保存会改为“手动记录”。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            form.platform.plans.forEach { plan ->
                FilterChip(
                    selected = form.planName == plan.name,
                    onClick = { viewModel.selectPlan(plan.name, plan.monthlyPriceUsd) },
                    label = { Text(plan.name) },
                )
            }
        }
        OutlinedTextField(
            value = form.planName,
            onValueChange = { v -> viewModel.update { it.copy(planName = v) } },
            label = { Text("套餐名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("订阅状态", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubscriptionStatus.entries.forEach { s ->
                FilterChip(
                    selected = form.status == s,
                    onClick = { viewModel.update { it.copy(status = s) } },
                    label = { Text(s.label) },
                )
            }
        }
        Text("计费周期", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BillingCycle.entries.forEach { c ->
                FilterChip(
                    selected = form.billingCycle == c,
                    onClick = { viewModel.update { it.copy(billingCycle = c) } },
                    label = { Text(c.label) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = form.price,
                onValueChange = { v -> viewModel.update { it.copy(price = v) } },
                label = { Text(if (form.billingCycle == BillingCycle.YEARLY) "每年费用" else "每月费用") },
                singleLine = true,
                isError = showErrors && form.priceError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = form.currency,
                onValueChange = { v -> viewModel.update { it.copy(currency = v.take(4)) } },
                label = { Text("币种") },
                singleLine = true,
                modifier = Modifier.weight(0.6f),
            )
        }
        Text(
            "预设价格仅供参考，请以账单为准。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DateField(
            label = "下次续费 / 到期日",
            value = form.renewsAt,
            onChange = { v -> viewModel.update { it.copy(renewsAt = v) } },
        )

        HorizontalDivider()
        Section("使用额度")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = form.quotaSource == QuotaSource.UNKNOWN,
                onClick = { viewModel.update { it.copy(quotaSource = QuotaSource.UNKNOWN) } },
                label = { Text("暂不记录") },
            )
            FilterChip(
                selected = form.quotaSource == QuotaSource.MANUAL,
                onClick = { viewModel.update { it.copy(quotaSource = QuotaSource.MANUAL) } },
                label = { Text("手动记录") },
            )
        }
        if (form.quotaSource == QuotaSource.MANUAL) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.quotaUsed,
                    onValueChange = { v -> viewModel.update { it.copy(quotaUsed = v.filter(Char::isDigit)) } },
                    label = { Text("已用") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("field_quota_used"),
                )
                OutlinedTextField(
                    value = form.quotaLimit,
                    onValueChange = { v -> viewModel.update { it.copy(quotaLimit = v.filter(Char::isDigit)) } },
                    label = { Text("总额度") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("field_quota_limit"),
                )
                OutlinedTextField(
                    value = form.quotaUnit,
                    onValueChange = { v -> viewModel.update { it.copy(quotaUnit = v) } },
                    label = { Text("单位") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            if (showErrors && form.quotaError != null) {
                Text(form.quotaError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            DateField(
                label = "额度重置日",
                value = form.quotaResetAt,
                onChange = { v -> viewModel.update { it.copy(quotaResetAt = v) } },
            )
        }
        Text(
            "ChatGPT、Claude、Gemini 没有公开的个人会员额度接口，这里的数字由你手动记录，界面会标注“手动记录”，不会冒充实时数据。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (form.platform.balanceSync) {
            HorizontalDivider()
            ApiBalanceSection(viewModel)
        }

        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("  私密备注（加密保存）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        OutlinedTextField(
            value = form.privateNote,
            onValueChange = { v -> viewModel.update { it.copy(privateNote = v) } },
            label = { Text("例如：绑定的手机号、付款方式") },
            minLines = 2,
            supportingText = { Text("使用 Android Keystore 加密，仅存于本机。请不要在这里保存密码。") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { openOfficialPage(context, form.platform.chatUrl) },
                modifier = Modifier.weight(1f),
            ) { Text("打开官方聊天") }
            OutlinedButton(
                onClick = { openOfficialPage(context, form.platform.billingUrl) },
                modifier = Modifier.weight(1f),
            ) { Text("查看订阅页") }
        }

        Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("btn_save")) {
            Text("保存")
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** DeepSeek 等提供官方余额接口的平台：用用户自己的 API Key 自动同步余额 */
@Composable
private fun ApiBalanceSection(viewModel: AccountEditViewModel) {
    val form = viewModel.form
    val context = LocalContext.current
    var showKey by remember { mutableStateOf(false) }

    Section("API 余额（自动同步）")
    Text(
        "填写你在 ${form.platform.displayName} 开放平台创建的 API Key，App 会调用官方余额接口查询，" +
            "数据标注为“自动同步”。Key 使用 Android Keystore 加密保存在本机，只发送给 ${form.platform.vendor} 官方接口，" +
            "保存后不会再显示。建议单独创建一个 Key 专门用于查询。网页版聊天是免费的，不需要填写。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val keyLabel = when {
        form.removeApiKey -> "保存后将移除已保存的 Key"
        viewModel.hasSavedApiKey -> "已保存 Key（留空保持不变，输入则替换）"
        else -> "API Key（sk-...）"
    }
    OutlinedTextField(
        value = form.apiKey,
        onValueChange = { v -> viewModel.update { it.copy(apiKey = v.trim(), removeApiKey = false) } },
        label = { Text(keyLabel) },
        singleLine = true,
        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            if (form.apiKey.isNotEmpty()) {
                TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "隐藏" else "显示") }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        form.platform.apiKeyUrl?.let { url ->
            OutlinedButton(onClick = { openOfficialPage(context, url) }) { Text("创建 API Key") }
        }
        if (viewModel.hasSavedApiKey && !form.removeApiKey) {
            TextButton(onClick = { viewModel.update { it.copy(apiKey = "", removeApiKey = true) } }) {
                Text("移除已保存的 Key", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    val stored = viewModel.stored
    if (stored != null && viewModel.hasSavedApiKey && !form.removeApiKey) {
        BalanceBlock(stored)
        OutlinedButton(
            onClick = {
                viewModel.syncNow { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
            },
            enabled = !viewModel.syncing,
        ) {
            Text(if (viewModel.syncing) "正在同步…" else "立即同步余额")
        }
    } else if (form.apiKey.isNotBlank()) {
        Text(
            "保存后会自动查询一次余额。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, value: Long?, onChange: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
            Text(if (value == null) "$label：未设置" else "$label：${formatDate(value)}")
        }
        if (value != null) {
            TextButton(onClick = { onChange(null) }) { Text("清除") }
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value?.let(::localMidnightToUtc))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(utcToLocalMidnight(it)) }
                    open = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("取消") } },
        ) {
            DatePicker(state = state)
        }
    }
}

/** DatePicker 使用 UTC 零点表示日期，这里与本地时区零点互相转换，避免跨时区差一天。 */
private fun utcToLocalMidnight(utcMillis: Long): Long =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun localMidnightToUtc(localMillis: Long): Long =
    Instant.ofEpochMilli(localMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

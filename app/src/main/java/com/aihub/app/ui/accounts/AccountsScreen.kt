package com.aihub.app.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.BillingCycle
import com.aihub.app.data.model.Platform
import com.aihub.app.ui.AppViewModelProvider
import com.aihub.app.ui.components.PlatformAvatar
import com.aihub.app.ui.components.StatusPill
import com.aihub.app.ui.components.openOfficialPage
import com.aihub.app.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onAddAccount: (Platform?) -> Unit,
    onOpenAccount: (Long) -> Unit,
    viewModel: AccountsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账号管理", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddAccount(state.filter) },
                modifier = Modifier.testTag("fab_add"),
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("添加账号") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.filter == null,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text("全部") },
                    )
                }
                items(Platform.entries) { p ->
                    FilterChip(
                        selected = state.filter == p,
                        onClick = { viewModel.setFilter(p) },
                        label = { Text(p.displayName) },
                    )
                }
            }

            if (state.groups.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.totalCount == 0) "还没有账号。\n点击右下角“添加账号”开始管理你的 AI 会员。" else "该平台下暂无账号",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.groups.forEach { (platform, accounts) ->
                        item(key = "h_${platform.name}") {
                            Text(
                                "${platform.displayName}（${accounts.size}）",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                            )
                        }
                        items(accounts, key = { it.id }) { account ->
                            AccountRow(
                                account = account,
                                onClick = { onOpenAccount(account.id) },
                                onOpenChat = { openOfficialPage(context, account.platform.chatUrl) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(account: AccountEntity, onClick: () -> Unit, onOpenChat: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            PlatformAvatar(account.platform)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        account.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    StatusPill(account.status)
                }
                if (account.email.isNotBlank()) {
                    Text(
                        account.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    buildString {
                        append(account.planName)
                        if (account.price > 0) {
                            append(" · ")
                            append(formatMoney(account.price, account.currency))
                            append(if (account.billingCycle == BillingCycle.YEARLY) "/年" else "/月")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onOpenChat) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "打开 ${account.platform.displayName} 官方聊天",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

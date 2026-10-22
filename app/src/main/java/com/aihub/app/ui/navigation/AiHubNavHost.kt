package com.aihub.app.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aihub.app.AiHubApp
import com.aihub.app.data.model.Platform
import com.aihub.app.ui.accounts.AccountsScreen
import com.aihub.app.ui.dashboard.DashboardScreen
import com.aihub.app.ui.edit.AccountEditScreen
import com.aihub.app.ui.edit.AccountEditViewModel
import com.aihub.app.ui.receipt.ReceiptImportScreen
import com.aihub.app.ui.settings.SettingsScreen

private enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "仪表盘", Icons.Filled.Home),
    ACCOUNTS("accounts", "账号", Icons.AutoMirrored.Filled.List),
    SETTINGS("settings", "设置", Icons.Filled.Settings),
}

private const val EDIT_ROUTE =
    "edit?${AccountEditViewModel.ARG_ID}={${AccountEditViewModel.ARG_ID}}" +
        "&${AccountEditViewModel.ARG_PLATFORM}={${AccountEditViewModel.ARG_PLATFORM}}"

private const val IMPORT_ROUTE = "import"

private fun NavHostController.openEditor(accountId: Long? = null, platform: Platform? = null) {
    val id = accountId ?: AccountEditViewModel.NEW_ID
    navigate("edit?${AccountEditViewModel.ARG_ID}=$id&${AccountEditViewModel.ARG_PLATFORM}=${platform?.name.orEmpty()}")
}

@Composable
fun AiHubNavHost(onSecureScreenChanged: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = TopLevel.entries.any { it.route == currentRoute }

    // 从其他 App 分享收据进来时，打开收据导入页（由导入页取走文字）
    val context = LocalContext.current
    val inbox = remember { (context.applicationContext as AiHubApp).container.sharedText }
    val pendingShare by inbox.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingShare) {
        if (pendingShare != null) navController.navigate(IMPORT_ROUTE) { launchSingleTop = true }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevel.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopLevel.DASHBOARD.route,
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                .consumeWindowInsets(padding),
        ) {
            composable(TopLevel.DASHBOARD.route) {
                DashboardScreen(
                    onAddAccount = { platform -> navController.openEditor(platform = platform) },
                    onOpenAccount = { id -> navController.openEditor(accountId = id) },
                    onImportReceipt = { navController.navigate(IMPORT_ROUTE) { launchSingleTop = true } },
                )
            }
            composable(TopLevel.ACCOUNTS.route) {
                AccountsScreen(
                    onAddAccount = { platform -> navController.openEditor(platform = platform) },
                    onOpenAccount = { id -> navController.openEditor(accountId = id) },
                )
            }
            composable(TopLevel.SETTINGS.route) {
                SettingsScreen(onSecureScreenChanged = onSecureScreenChanged)
            }
            composable(
                route = EDIT_ROUTE,
                arguments = listOf(
                    navArgument(AccountEditViewModel.ARG_ID) {
                        type = NavType.LongType
                        defaultValue = AccountEditViewModel.NEW_ID
                    },
                    navArgument(AccountEditViewModel.ARG_PLATFORM) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                AccountEditScreen(onDone = { navController.popBackStack() })
            }
            composable(IMPORT_ROUTE) {
                ReceiptImportScreen(
                    onBack = { navController.popBackStack() },
                    // 写入后打开该账号，方便核对识别结果
                    onImported = { id ->
                        navController.popBackStack()
                        navController.openEditor(accountId = id)
                    },
                )
            }
        }
    }
}

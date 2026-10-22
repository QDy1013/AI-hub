package com.aihub.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aihub.app.AiHubApp
import com.aihub.app.AppContainer
import com.aihub.app.ui.accounts.AccountsViewModel
import com.aihub.app.ui.dashboard.DashboardViewModel
import com.aihub.app.ui.edit.AccountEditViewModel
import com.aihub.app.ui.receipt.ReceiptImportViewModel

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { DashboardViewModel(container().accountRepository) }
        initializer { AccountsViewModel(container().accountRepository) }
        initializer {
            AccountEditViewModel(createSavedStateHandle(), container().accountRepository, container().appScope)
        }
        initializer { ReceiptImportViewModel(container().accountRepository, container().sharedText) }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as AiHubApp).container

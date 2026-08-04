package com.allowance.manager.feature.account

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.usecase.account.AddAccountUseCase
import com.allowance.manager.core.domain.usecase.account.DeleteAccountUseCase
import com.allowance.manager.core.domain.usecase.account.ObserveAccountsUseCase
import com.allowance.manager.core.domain.usecase.account.UpdateAccountUseCase
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.AnalyticsHelper
import com.allowance.manager.core.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val accounts: List<Account> = emptyList(),
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    observeAccountsUseCase: ObserveAccountsUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val analytics: AnalyticsHelper,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAccountsUseCase().collect { list ->
                _uiState.update { it.copy(accounts = list) }
            }
        }
    }

    fun onAdd(bankName: String, pattern: String) {
        if (pattern.isBlank()) return
        analytics.logEvent(AmAnalytics.Event.ACCOUNT_ADD)
        viewModelScope.launch {
            addAccountUseCase(
                Account(
                    packageName = "",
                    bankName = bankName.ifBlank { "내 계좌" },
                    accountPattern = pattern,
                    enabled = true,
                )
            )
        }
    }

    fun onToggleEnabled(account: Account, enabled: Boolean) {
        analytics.logEvent(AmAnalytics.Event.ACCOUNT_ENABLED_TOGGLE, mapOf(AmAnalytics.Param.ENABLED to enabled))
        viewModelScope.launch { updateAccountUseCase(account.copy(enabled = enabled)) }
    }

    fun onDelete(account: Account) {
        analytics.logEvent(AmAnalytics.Event.ACCOUNT_DELETE)
        viewModelScope.launch { deleteAccountUseCase(account) }
    }
}

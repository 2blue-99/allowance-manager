package com.allowance.manager.feature.account

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.usecase.account.AddAccountUseCase
import com.allowance.manager.core.domain.usecase.account.DeleteAccountUseCase
import com.allowance.manager.core.domain.usecase.account.ObserveAccountsUseCase
import com.allowance.manager.core.domain.usecase.account.UpdateAccountUseCase
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
    val editing: Account? = null,       // 수정 다이얼로그 대상 (null = 닫힘)
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    observeAccountsUseCase: ObserveAccountsUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
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
        viewModelScope.launch {
            addAccountUseCase(
                Account(
                    packageName = "",
                    bankName = bankName.ifBlank { "내 계좌" },
                    accountPattern = pattern.trim(),
                    enabled = true,
                )
            )
        }
    }

    fun onToggleEnabled(account: Account, enabled: Boolean) {
        viewModelScope.launch { updateAccountUseCase(account.copy(enabled = enabled)) }
    }

    fun onDelete(account: Account) {
        viewModelScope.launch { deleteAccountUseCase(account) }
    }

    fun onStartEdit(account: Account) = _uiState.update { it.copy(editing = account) }
    fun onCancelEdit() = _uiState.update { it.copy(editing = null) }

    fun onEditChange(bankName: String, pattern: String) = _uiState.update {
        it.copy(editing = it.editing?.copy(bankName = bankName, accountPattern = pattern))
    }

    fun onSaveEdit() {
        val editing = _uiState.value.editing ?: return
        viewModelScope.launch {
            updateAccountUseCase(editing.copy(accountPattern = editing.accountPattern.trim()))
            _uiState.update { it.copy(editing = null) }
        }
    }
}

package com.allowance.manager.feature.setting

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.analytics.AnalyticsHelper
import com.allowance.manager.core.common.BaseViewModel
import com.allowance.manager.core.domain.model.IgnoredAccount
import com.allowance.manager.core.domain.usecase.ignore.ObserveIgnoredAccountsUseCase
import com.allowance.manager.core.domain.usecase.ignore.RemoveIgnoredAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IgnoredAccountUiState(
    val accounts: List<IgnoredAccount> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class IgnoredAccountViewModel @Inject constructor(
    observeIgnoredAccountsUseCase: ObserveIgnoredAccountsUseCase,
    private val removeIgnoredAccountUseCase: RemoveIgnoredAccountUseCase,
    private val analytics: AnalyticsHelper,
) : BaseViewModel() {

    val uiState: StateFlow<IgnoredAccountUiState> = observeIgnoredAccountsUseCase()
        .map { IgnoredAccountUiState(accounts = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IgnoredAccountUiState())

    fun onRemove(id: Long) {
        viewModelScope.launch { removeIgnoredAccountUseCase(id) }
    }
}

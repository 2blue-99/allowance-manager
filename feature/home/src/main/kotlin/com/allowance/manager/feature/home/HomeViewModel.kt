package com.allowance.manager.feature.home

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.usecase.balance.GetBalanceUseCase
import com.allowance.manager.core.domain.usecase.balance.SetBalanceUseCase
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val balance: String = "",

    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getBalanceUseCase: GetBalanceUseCase,
    private val setBalanceUseCase: SetBalanceUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getBalanceUseCase().collect { amount ->
                _uiState.update { it.copy(balance = amount.amountToComma()) }
            }
        }
    }

    fun setAmount() {
        viewModelScope.launch {
            setBalanceUseCase(1000)
        }
    }
}

package com.allowance.manager.feature.home

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.usecase.store.GetMonthAllowanceUseCase
import com.allowance.manager.core.domain.usecase.store.SetMonthAllowanceUseCase
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val balance: String = "",
    val inputAmount: String = "",

    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMonthAllowanceUseCase: GetMonthAllowanceUseCase,
    private val setMonthAllowanceUseCase: SetMonthAllowanceUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMonthAllowanceUseCase().collect { amount ->
                _uiState.update { it.copy(balance = amount.amountToComma()) }
            }
        }
    }

    fun onInputAmountChange(value: String) {
        if (value.all { it.isDigit() }) {
            _uiState.update { it.copy(inputAmount = value) }
        }
    }

    fun saveAmount() {
        val amount = _uiState.value.inputAmount.toLongOrNull() ?: return
        viewModelScope.launch {
            setMonthAllowanceUseCase(amount)
            _uiState.update { it.copy(inputAmount = "") }
        }
    }
}

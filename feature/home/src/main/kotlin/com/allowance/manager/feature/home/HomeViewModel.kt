package com.allowance.manager.feature.home

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.model.Spending
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.usecase.spending.GetAllSpendingsUseCase
import com.allowance.manager.core.domain.usecase.spending.InsertSpendingUseCase
import com.allowance.manager.core.domain.usecase.store.CalculateDailyAllowanceUseCase
import com.allowance.manager.core.domain.usecase.store.GetAllowanceInfoUseCase
import com.allowance.manager.core.domain.usecase.store.SetDailyAllowanceUseCase
import com.allowance.manager.core.domain.usecase.store.SetMonthAllowanceUseCase
import com.allowance.manager.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val dailyAllowance: Long = 0L,
    val dailyRemaining: Long = 0L,
    val monthAllowance: Long = 0L,
    val monthSpent: Long = 0L,
    val monthRemaining: Long = 0L,
    val daysUntilPayday: Long = 0L,
    val todaySpendings: List<Spending> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllowanceInfoUseCase: GetAllowanceInfoUseCase,
    private val getAllSpendingsUseCase: GetAllSpendingsUseCase,
    private val setMonthAllowanceUseCase: SetMonthAllowanceUseCase,
    private val calculateDailyAllowanceUseCase: CalculateDailyAllowanceUseCase,
    private val insertSpendingUseCase: InsertSpendingUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun saveMonthAllowance(amount: Long) {
        viewModelScope.launch {
            setMonthAllowanceUseCase(amount)
            insertSpendingUseCase(Spending.initSpending(amount))
            calculateDailyAllowanceUseCase()
        }
    }

    init {
        viewModelScope.launch {
            combine(
                getAllowanceInfoUseCase(),
                getAllSpendingsUseCase(),
            ) { allowance, spendings ->
                val today = LocalDate.now()
                val todaySpendings = spendings.filter { spending ->
                    Instant.ofEpochMilli(spending.timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate() == today
                }
                val todaySpent = todaySpendings
                    .filter { it.type == TransactionType.SPEND }
                    .sumOf { it.amount }
                val monthSpent = spendings
                    .filter { it.type == TransactionType.SPEND }
                    .sumOf { it.amount }
                val monthRemaining = (allowance.monthAllowance - monthSpent).coerceAtLeast(0L)

                HomeUiState(
                    dailyAllowance = allowance.dailyAllowance,
                    dailyRemaining = (allowance.dailyAllowance - todaySpent).coerceAtLeast(0L),
                    monthAllowance = allowance.monthAllowance,
                    monthSpent = monthSpent,
                    monthRemaining = monthRemaining,
                    daysUntilPayday = allowance.leftDays,
                    todaySpendings = todaySpendings,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}

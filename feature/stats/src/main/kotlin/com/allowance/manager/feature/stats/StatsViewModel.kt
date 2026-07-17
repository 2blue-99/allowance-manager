package com.allowance.manager.feature.stats

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.usecase.stats.GetMonthlyExpenseTotalsUseCase
import com.allowance.manager.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class MonthBar(
    val label: String,      // "7월"
    val total: Long,
    val isCurrent: Boolean,
)

data class StatsUiState(
    val currentMonthTotal: Long = 0L,
    val prevMonthDiff: Long = 0L,   // 이번달 - 지난달 (양수 = 더 씀)
    val bars: List<MonthBar> = emptyList(),
    val isLoading: Boolean = true,
)

private const val MONTHS_BACK = 6

@HiltViewModel
class StatsViewModel @Inject constructor(
    getMonthlyExpenseTotalsUseCase: GetMonthlyExpenseTotalsUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMonthlyExpenseTotalsUseCase().collect { totals ->
                val map = totals.associate { it.yearMonth to it.total }
                val now = YearMonth.now()
                val recent = (0 until MONTHS_BACK).map { i ->
                    now.minusMonths((MONTHS_BACK - 1 - i).toLong())
                }
                val bars = recent.map { ym ->
                    MonthBar(
                        label = "${ym.monthValue}월",
                        total = map[ym] ?: 0L,
                        isCurrent = ym == now,
                    )
                }
                val current = map[now] ?: 0L
                val prev = map[now.minusMonths(1)] ?: 0L
                _uiState.value = StatsUiState(
                    currentMonthTotal = current,
                    prevMonthDiff = current - prev,
                    bars = bars,
                    isLoading = false,
                )
            }
        }
    }
}

package com.allowance.manager.core.domain.usecase.calendar

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.repository.CycleRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** 그 사이클 기간의 입출금 내역을 최신순으로 관찰. 월별 화면·사이클 이동에 쓴다. */
class ObserveCycleTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(cycle: BudgetCycle): Flow<List<Transaction>> =
        transactionRepository.observeBetween(cycle.startMillis(), cycle.endMillis())
}

/**
 * [from]에서 [offset]칸 떨어진 사이클. 음수면 과거, 양수면 미래.
 *
 * 사이클이 행으로 저장돼 있으므로 목록에서 이웃 행을 찾는다.
 * 목록 밖으로는 더 가지 않고 끝(첫/마지막 행)에서 멈춘다 — 호출부는 시작일이 같으면 정지로 판정한다.
 */
class GetAdjacentCycleUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
) {
    suspend operator fun invoke(from: BudgetCycle, offset: Int): BudgetCycle {
        if (offset == 0) return from
        val periods = cycleRepository.getAll().map { it.period }
        if (periods.isEmpty()) return from
        // from이 행 목록에 없으면(가상 과거 사이클 등) 시작일 기준 가장 가까운 이전 행으로 잡는다
        val index = periods.indexOfFirst { it.start == from.start }
            .takeIf { it >= 0 }
            ?: periods.indexOfLast { it.start.isBefore(from.start) }.coerceAtLeast(0)
        return periods[(index + offset).coerceIn(0, periods.lastIndex)]
    }
}

/**
 * 거래가 하나라도 있는 사이클 목록(최신순). 사이클 피커에서 데이터 있는 것만 고르게 한다.
 *
 * 사이클은 달력 월과 1:1이 아니라(규칙을 바꾸면 한 달에 둘이 시작하거나 없는 달도 생긴다)
 * 격자로 못 그리므로 목록으로 준다. 사이클 행·거래 어느 쪽이 바뀌어도 다시 계산된다.
 */
class ObserveTransactionCyclesUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(): Flow<List<BudgetCycle>> =
        combine(
            cycleRepository.observeAll(),
            transactionRepository.observeAllTimes(),
        ) { cycles, times ->
            cycles.map { it.period }
                .filter { period -> times.any { it in period.startMillis()..period.endMillis() } }
                .sortedByDescending { it.start }
        }
}

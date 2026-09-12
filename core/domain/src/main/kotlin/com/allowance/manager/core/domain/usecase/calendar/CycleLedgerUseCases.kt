package com.allowance.manager.core.domain.usecase.calendar

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.repository.CycleRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
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
 * 행으로 저장된 사이클은 이웃 행으로, **첫 행보다 앞은 첫 행의 규칙으로 역산한 가상 사이클**로 이어진다
 * (저장소가 첫 행 이전 날짜를 조회할 때와 같은 방식). 통계 창이 데이터가 적어도 항상 6칸을 채울 수 있게 —
 * 가상 사이클은 거래가 없어 선택 불가·예산 0으로 표시된다.
 * 미래로는 마지막 행(오늘 사이클)에서 멈춘다 — 호출부는 시작일이 같으면 정지로 판정한다.
 */
class GetAdjacentCycleUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    suspend operator fun invoke(from: BudgetCycle, offset: Int): BudgetCycle {
        if (offset == 0) return from
        val rows = cycleRepository.getAll()
        if (rows.isEmpty()) return from
        val first = rows.first()
        val holidays = remoteConfigRepository.getHolidays()

        // 행 목록 앞에 가상 사이클을 필요한 만큼 이어 붙인 체인. 뒤로 한 칸 늘릴 수 없으면 false
        val chain = ArrayDeque(rows.map { it.period })
        fun extendBack(): Boolean {
            val head = chain.first()
            val start = BudgetCycle.previousPayDateBefore(head.start, first.payday, holidays)
            if (!start.isBefore(head.start)) return false
            chain.addFirst(BudgetCycle(start, head.start))
            return true
        }

        // from이 체인에 들어올 때까지 앞으로 확장 (가상 과거 사이클에서 출발하는 경우)
        var probes = 0
        while (from.start.isBefore(chain.first().start) && probes++ < MAX_VIRTUAL) {
            if (!extendBack()) break
        }

        var index = chain.indexOfFirst { it.start == from.start }
            .takeIf { it >= 0 }
            ?: chain.indexOfLast { it.start.isBefore(from.start) }   // 체인에 없으면 가장 가까운 이전 사이클 기준
        var target = index + offset
        // 목표가 체인 앞을 넘으면 그만큼 더 역산
        probes = 0
        while (target < 0 && probes++ < MAX_VIRTUAL) {
            if (!extendBack()) break
            index++
            target++
        }
        return chain[target.coerceIn(0, chain.lastIndex)]
    }

    private companion object {
        /** 가상 사이클 역산 한도 — 무한 루프 방지 (월 주기 기준 50년) */
        const val MAX_VIRTUAL = 600
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

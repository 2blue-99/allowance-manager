package com.allowance.manager.core.domain.usecase.calendar

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.repository.DataStoreRepository
import com.allowance.manager.core.domain.repository.PaydayRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** 그 사이클의 입출금 내역을 최신순으로 관찰. 월별 화면·사이클 이동에 쓴다. */
class ObserveCycleTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(cycleStart: LocalDate): Flow<List<Transaction>> =
        transactionRepository.observeByCycle(cycleStart)
}

/**
 * [from]에서 [offset]칸 떨어진 사이클. 음수면 과거, 양수면 미래.
 *
 * 경계가 맞물려 있으므로 시작 하루 전(과거) · 끝(미래)으로 되짚으면 빈틈 없이 이동한다.
 * 이력의 첫 사이클보다 과거로는 더 가지 않는다.
 */
class GetAdjacentCycleUseCase @Inject constructor(
    private val paydayRepository: PaydayRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    suspend operator fun invoke(from: BudgetCycle, offset: Int): BudgetCycle {
        if (offset == 0) return from
        val rules = paydayRepository.history()
        val holidays = remoteConfigRepository.getHolidays()
        val fallbackPayday = dataStoreRepository.getPayday().first()

        var current = from
        repeat(kotlin.math.abs(offset)) {
            val probe = if (offset < 0) current.start.minusDays(1) else current.endExclusive
            val next = BudgetCycle.of(rules, probe, holidays, fallbackPayday)
            // 더 이동할 수 없으면(이력의 첫 사이클) 그대로 멈춘다
            if (next.start == current.start) return current
            current = next
        }
        return current
    }
}

/**
 * 거래가 하나라도 있는 사이클 목록(최신순). 사이클 피커에서 데이터 있는 것만 고르게 한다.
 *
 * 사이클은 달력 월과 1:1이 아니라(규칙을 바꾸면 한 달에 둘이 시작하거나 없는 달도 생긴다)
 * 격자로 못 그리므로 목록으로 준다.
 */
class ObserveTransactionCyclesUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val paydayRepository: PaydayRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    operator fun invoke(): Flow<List<BudgetCycle>> =
        transactionRepository.observeTransactionCycles().map { starts ->
            val rules = paydayRepository.history()
            val holidays = remoteConfigRepository.getHolidays()
            val fallbackPayday = dataStoreRepository.getPayday().first()
            starts.map { BudgetCycle.of(rules, it, holidays, fallbackPayday) }
                .distinctBy { it.start }
        }
}

package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.model.BudgetCycle
import com.allowance.manager.core.domain.repository.CycleRepository
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * 월급일 변경을 저장했을 때의 결과 예고.
 *
 * @param thisCycle 경계부터 새 규칙으로 계산한 끝까지 — 저장 후 "이번 회차"
 * @param previousCycle 경계 직전 사이클이 경계에서 끊긴 모습. 첫 사이클이라 직전이 없으면 null
 */
data class PaydayChangePreview(
    val thisCycle: BudgetCycle,
    val previousCycle: BudgetCycle?,
) {
    /** 다음 회차가 시작하는 날 (= 이번 회차 끝) */
    val nextStart: LocalDate get() = thisCycle.endExclusive
}

/**
 * [MoveCycleStartUseCase]의 결과 예고 — 시작만 [boundary]로 옮기고 **끝은 현재 회차 것을 유지**.
 *
 * 저장 로직([CycleRepository.moveCycleStart])과 같은 규칙: 직전 회차 끝은 경계로, 이번 회차 끝은 그대로.
 * 경계가 현재 끝을 넘는 비정상 입력은 저장 쪽과 똑같이 규칙으로 끝을 계산한다.
 */
class PreviewCycleStartUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    suspend operator fun invoke(boundary: LocalDate, today: LocalDate = LocalDate.now()): PaydayChangePreview {
        val current = cycleRepository.cycleAt(today)
        val previous = cycleRepository.getAll().lastOrNull { it.start.isBefore(boundary) }
        val end = if (boundary.isBefore(current.endExclusive)) {
            current.endExclusive
        } else {
            BudgetCycle.endAfterPayDate(boundary, current.payday, remoteConfigRepository.getHolidays())
        }
        return PaydayChangePreview(
            thisCycle = BudgetCycle(boundary, end),
            previousCycle = previous?.let { BudgetCycle(it.start, boundary) },
        )
    }
}

/**
 * [ChangePaydayUseCase]를 실제로 저장하기 전에 결과를 계산만 해서 보여준다.
 *
 * 저장 로직([CycleRepository.changePayday])과 같은 규칙을 따른다 — 경계 이후 사이클은 대체되고,
 * 직전 사이클의 끝이 경계로 맞춰지며, 새 회차의 끝은 규칙일(주말·공휴일 보정)로 계산된다.
 * 다이얼로그가 이 결과를 그대로 띄우므로 "예고와 저장 결과가 다르다"는 일이 생기지 않는다.
 */
class PreviewPaydayChangeUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    suspend operator fun invoke(boundary: LocalDate, payday: Int): PaydayChangePreview {
        val holidays = remoteConfigRepository.getHolidays()
        val previous = cycleRepository.getAll().lastOrNull { it.start.isBefore(boundary) }
        return PaydayChangePreview(
            thisCycle = BudgetCycle(boundary, BudgetCycle.endAfterPayDate(boundary, payday.coerceIn(0, 31), holidays)),
            previousCycle = previous?.let { BudgetCycle(it.start, boundary) },
        )
    }
}

package com.allowance.manager.core.domain.usecase.budget

import com.allowance.manager.core.domain.repository.CycleRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * 온보딩·초기 설정에서 첫 사이클을 심는다 — 오늘이 속한 사이클을 규칙일로 계산해 행으로 저장.
 * 예산은 이후 [SetMonthlyBudgetUseCase]가 이 행에 쓴다.
 */
class InitPaydayRuleUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
) {
    suspend operator fun invoke(payday: Int, today: LocalDate = LocalDate.now()) =
        cycleRepository.init(payday, today)
}

/**
 * 월급일 변경 적용 — 시트에서 **규칙일**과 **이번에 받은 날(경계)** 을 함께 받는다.
 *
 * 경계를 그대로 두면 규칙일만 갱신되어 시작일(이미 받은 날)은 유지되고 끝만 다시 계산되고,
 * 경계를 옮기면 그 날부터 새 사이클이 시작한다(거래 소속은 날짜 조회라 자동으로 따라온다).
 */
class ChangePaydayUseCase @Inject constructor(
    private val cycleRepository: CycleRepository,
) {
    suspend operator fun invoke(boundary: LocalDate, payday: Int, today: LocalDate = LocalDate.now()) =
        cycleRepository.changePayday(boundary, payday, today)
}

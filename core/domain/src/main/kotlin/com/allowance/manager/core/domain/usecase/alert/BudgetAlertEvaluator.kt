package com.allowance.manager.core.domain.usecase.alert

/**
 * 예산 소진 알림의 임계값 관통 판정(순수 로직, Android 비의존).
 *
 * 임계값은 "남은 예산 %"(0 = 다 씀). 남은 비율이 임계값 이하로 **처음** 내려갈 때 알림 대상이 된다.
 * - 한 번에 여러 임계값을 관통하면 **가장 심각한 것(가장 낮은 남은 %) 하나만** 발송한다.
 * - 남은 비율이 임계값 위로 회복되면 그 임계값은 자동으로 재장전(다음에 다시 발송 가능)된다.
 *   → 새 fired 집합 = "현재 관통 중인 임계값 전체"로 두면 회복분이 자연히 빠진다.
 */
object BudgetAlertEvaluator {

    data class Result(
        /** 이번에 발송할 임계값(남은 %). null이면 발송 없음. */
        val thresholdToFire: Int?,
        /** 저장할 새 fired 집합(= 현재 관통 중인 임계값 전체). */
        val newFired: Set<Int>,
    )

    /**
     * @param remainingPercent 남은 예산 비율(%). 100 초과(수입 가산)·음수(초과) 가능.
     * @param thresholds 빈도별 "남은 %" 임계값 목록(0 포함).
     * @param fired 이번 사이클에서 이미 발송한 임계값 집합.
     */
    fun evaluate(remainingPercent: Float, thresholds: List<Int>, fired: Set<Int>): Result {
        // 현재 관통 중(남은 비율이 임계값 이하)인 임계값 전체
        val reached = thresholds.filter { remainingPercent <= it }.toSet()
        // 아직 발송 안 한 관통값 → 그중 가장 심각한(가장 낮은) 하나만 발송
        val newly = reached - fired
        return Result(thresholdToFire = newly.minOrNull(), newFired = reached)
    }
}

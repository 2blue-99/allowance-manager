package com.allowance.manager.core.domain.usecase.alert

import com.allowance.manager.core.domain.model.AlertFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetAlertEvaluatorTest {

    private val medium = AlertFrequency.MEDIUM.remainingThresholds  // 70,50,30,10,0

    @Test
    fun `남은 비율이 아직 최고 임계값 위면 발송 없음`() {
        val r = BudgetAlertEvaluator.evaluate(remainingPercent = 85f, thresholds = medium, fired = emptySet())
        assertNull(r.thresholdToFire)
        assertEquals(emptySet<Int>(), r.newFired)
    }

    @Test
    fun `첫 관통이면 해당 임계값 발송`() {
        val r = BudgetAlertEvaluator.evaluate(remainingPercent = 65f, thresholds = medium, fired = emptySet())
        assertEquals(70, r.thresholdToFire)
        assertEquals(setOf(70), r.newFired)
    }

    @Test
    fun `한 번에 여러 임계값 관통 시 가장 심각한 것 하나만 발송`() {
        // 100% -> 0% 급락: 전 임계값 관통, "다 씀"(0)만 발송, 나머지는 fired 처리
        val r = BudgetAlertEvaluator.evaluate(remainingPercent = 0f, thresholds = medium, fired = emptySet())
        assertEquals(0, r.thresholdToFire)
        assertEquals(setOf(70, 50, 30, 10, 0), r.newFired)
    }

    @Test
    fun `이미 발송한 임계값은 재발송하지 않음`() {
        val r = BudgetAlertEvaluator.evaluate(remainingPercent = 65f, thresholds = medium, fired = setOf(70))
        assertNull(r.thresholdToFire)
        assertEquals(setOf(70), r.newFired)
    }

    @Test
    fun `남은 비율이 회복되면 임계값이 재장전된다`() {
        // 65%였다가(70 fired) 이체를 되돌려 80%로 회복 → 70이 fired에서 빠짐
        val recovered = BudgetAlertEvaluator.evaluate(remainingPercent = 80f, thresholds = medium, fired = setOf(70))
        assertNull(recovered.thresholdToFire)
        assertEquals(emptySet<Int>(), recovered.newFired)

        // 다시 65%로 내려가면 70 재발송
        val again = BudgetAlertEvaluator.evaluate(remainingPercent = 65f, thresholds = medium, fired = recovered.newFired)
        assertEquals(70, again.thresholdToFire)
    }

    @Test
    fun `초과(음수)면 다 씀 발송`() {
        val r = BudgetAlertEvaluator.evaluate(remainingPercent = -20f, thresholds = medium, fired = setOf(70, 50, 30, 10))
        assertEquals(0, r.thresholdToFire)
        assertEquals(setOf(70, 50, 30, 10, 0), r.newFired)
    }
}

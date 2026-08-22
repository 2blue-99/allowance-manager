package com.allowance.manager.core.domain.model

/**
 * 예산 소진 알림 빈도 — 온보딩·설정에서 선택.
 * 임계값은 "남은 예산 %" 기준(0 = 다 씀). 남은 비율이 각 값 아래로 처음 내려갈 때 알림을 쏜다.
 * 빈도가 높을수록 임계값이 촘촘해지며, 세 세트는 서로 중첩된다(LOW ⊂ MEDIUM ⊂ HIGH).
 *
 * - key: DataStore 저장 키 (low/medium/high)
 * - label: 화면 노출 이름
 * - hint: 선택지 보조 문구
 * - remainingThresholds: 알림을 쏠 "남은 %" 목록(내림차순, 0 = 다 씀 포함)
 */
enum class AlertFrequency(
    val key: String,
    val label: String,
    val hint: String,
    val remainingThresholds: List<Int>,
) {
    LOW("low", "낮음", "꼭 필요할 때만", listOf(50, 10, 0)),
    MEDIUM("medium", "중간", "적당하게", listOf(70, 50, 30, 10, 0)),
    HIGH("high", "높음", "촘촘하게", listOf(90, 70, 50, 30, 20, 10, 0));

    /** 기본(추천) 여부 */
    val recommended: Boolean get() = this == Default

    /** 선택지 라벨 — 추천 항목은 "중간  ·  추천" 형태 */
    val displayLabel: String get() = if (recommended) "$label  ·  추천" else label

    /** 선택지 보조 문구 — "적당하게 · 70·50·30·10% · 다 씀" */
    val summary: String
        get() = "$hint · ${remainingThresholds.filter { it > 0 }.joinToString("·")}% · 다 씀"

    companion object {
        val Default = MEDIUM

        fun fromKey(key: String?): AlertFrequency = entries.firstOrNull { it.key == key } ?: Default
    }
}

/**
 * 예산 소진 알림 설정.
 * @param enabled 알림 수신 여부(온보딩·설정의 토글). 꺼지면 빈도는 무시된다.
 * @param frequency 알림 빈도(임계값 세트).
 */
data class BudgetAlertSetting(
    val enabled: Boolean = true,
    val frequency: AlertFrequency = AlertFrequency.Default,
)

/**
 * 예산 소진 알림의 발송 상태(내부용, 사용자 설정 아님).
 * 프로세스 재시작에도 중복 발송을 막기 위해 저장한다.
 * @param cycleStart 현재 사이클 시작 millis. 사이클이 바뀌면 [fired]를 초기화한다.
 * @param fired 이번 사이클에서 이미 발송(관통)한 "남은 %" 임계값 집합.
 */
data class BudgetAlertState(
    val cycleStart: Long = 0L,
    val fired: Set<Int> = emptySet(),
)

/**
 * 가계부 관리 알림(매일 정산 리마인더) 설정.
 * 그날 기록된 내역이 있으면 지정 시각에 확인 알림을 보낸다.
 * @param enabled 알림 수신 여부.
 * @param hour 알림 시각(0~23).
 * @param minute 알림 분(0~59).
 */
data class DailyReminderSetting(
    val enabled: Boolean = true,
    val hour: Int = 21,
    val minute: Int = 0,
) {
    /** 하루 자정 기준 분(0~1439) — DataStore 저장/복원용. */
    val minutesOfDay: Int get() = hour * 60 + minute

    companion object {
        const val DEFAULT_MINUTES = 21 * 60  // 오후 9:00

        fun fromMinutes(enabled: Boolean, minutesOfDay: Int): DailyReminderSetting {
            val m = minutesOfDay.coerceIn(0, 24 * 60 - 1)
            return DailyReminderSetting(enabled = enabled, hour = m / 60, minute = m % 60)
        }
    }
}

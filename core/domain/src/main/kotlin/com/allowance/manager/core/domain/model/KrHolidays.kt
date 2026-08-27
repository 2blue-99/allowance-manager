package com.allowance.manager.core.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * 비영업일(공휴일·대체공휴일·선거일·은행휴무) 목록의 파서와 내장 폴백.
 *
 * 설날·추석처럼 매년 날짜가 바뀌어 코드로 계산할 수 없으므로 Remote Config(`kr_holidays`)로 내려주고,
 * 값을 못 받았을 때만 [FALLBACK]을 쓴다. 원본 JSON은 저장소의 `remote-config/kr-holidays.json`.
 *
 * 근로자의 날(5/1)은 관공서 공휴일이 아니지만 **은행이 쉬어 급여가 앞당겨 지급되므로** 포함한다.
 */
object KrHolidays {

    @Serializable
    private data class HolidayEntry(val date: String, val name: String = "")

    @Serializable
    private data class Payload(val version: Int = 0, val holidays: List<HolidayEntry> = emptyList())

    // 사람이 검수용으로 넣는 필드(type·dayOfWeek·description)를 앱은 무시한다.
    // 특히 dayOfWeek는 날짜에서 계산되는 값이라, 원본이 틀렸을 때 앱이 그걸 믿으면 안 된다.
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Remote Config 문자열 → [Holidays]. 형식이 깨지면 빈 값을 주고, 호출부가 [FALLBACK]으로 넘어간다.
     * (원격 값 오타 하나로 사이클 경계가 망가지면 안 된다)
     */
    fun parse(raw: String): Holidays {
        if (raw.isBlank()) return Holidays.EMPTY
        val payload = runCatching { json.decodeFromString<Payload>(raw) }.getOrNull() ?: return Holidays.EMPTY
        val byDate = payload.holidays
            .mapNotNull { entry ->
                runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { it to entry.name }
            }
            .toMap()
        return if (byDate.isEmpty()) Holidays.EMPTY else Holidays(byDate, payload.version)
    }

    /**
     * Remote Config 미수신 시 쓰는 내장 목록(2026~2028). version = 0 이라 디버그 화면에서 폴백임이 드러난다.
     * ⚠️ 오프라인·최초 실행 대비용 안전망이다. 정확한 값은 Remote Config로 관리한다.
     */
    val FALLBACK: Holidays = Holidays(
        byDate = mapOf(
            LocalDate.of(2026, 1, 1) to "신정",
            LocalDate.of(2026, 2, 16) to "설날 연휴",
            LocalDate.of(2026, 2, 17) to "설날",
            LocalDate.of(2026, 2, 18) to "설날 연휴",
            LocalDate.of(2026, 3, 1) to "삼일절",
            LocalDate.of(2026, 3, 2) to "대체공휴일 (삼일절)",
            LocalDate.of(2026, 5, 1) to "근로자의 날",
            LocalDate.of(2026, 5, 5) to "어린이날",
            LocalDate.of(2026, 5, 24) to "부처님오신날",
            LocalDate.of(2026, 5, 25) to "대체공휴일 (부처님오신날)",
            LocalDate.of(2026, 6, 3) to "제9회 전국동시지방선거",
            LocalDate.of(2026, 6, 6) to "현충일",
            LocalDate.of(2026, 8, 15) to "광복절",
            LocalDate.of(2026, 8, 17) to "대체공휴일 (광복절)",
            LocalDate.of(2026, 9, 24) to "추석 연휴",
            LocalDate.of(2026, 9, 25) to "추석",
            LocalDate.of(2026, 9, 26) to "추석 연휴",
            LocalDate.of(2026, 10, 3) to "개천절",
            LocalDate.of(2026, 10, 5) to "대체공휴일 (개천절)",
            LocalDate.of(2026, 10, 9) to "한글날",
            LocalDate.of(2026, 12, 25) to "성탄절",
            LocalDate.of(2027, 1, 1) to "신정",
            LocalDate.of(2027, 2, 6) to "설날 연휴",
            LocalDate.of(2027, 2, 7) to "설날",
            LocalDate.of(2027, 2, 8) to "설날 연휴",
            LocalDate.of(2027, 2, 9) to "대체공휴일 (설날)",
            LocalDate.of(2027, 3, 1) to "삼일절",
            LocalDate.of(2027, 5, 1) to "근로자의 날",
            LocalDate.of(2027, 5, 5) to "어린이날",
            LocalDate.of(2027, 5, 13) to "부처님오신날",
            LocalDate.of(2027, 6, 6) to "현충일",
            LocalDate.of(2027, 8, 15) to "광복절",
            LocalDate.of(2027, 8, 16) to "대체공휴일 (광복절)",
            LocalDate.of(2027, 9, 14) to "추석 연휴",
            LocalDate.of(2027, 9, 15) to "추석",
            LocalDate.of(2027, 9, 16) to "추석 연휴",
            LocalDate.of(2027, 10, 3) to "개천절",
            LocalDate.of(2027, 10, 4) to "대체공휴일 (개천절)",
            LocalDate.of(2027, 10, 9) to "한글날",
            LocalDate.of(2027, 10, 11) to "대체공휴일 (한글날)",
            LocalDate.of(2027, 12, 25) to "성탄절",
            LocalDate.of(2027, 12, 27) to "대체공휴일 (성탄절)",
            LocalDate.of(2028, 1, 1) to "신정",
            LocalDate.of(2028, 1, 26) to "설날 연휴",
            LocalDate.of(2028, 1, 27) to "설날",
            LocalDate.of(2028, 1, 28) to "설날 연휴",
            LocalDate.of(2028, 3, 1) to "삼일절",
            LocalDate.of(2028, 4, 12) to "제23대 국회의원선거",
            LocalDate.of(2028, 5, 1) to "근로자의 날",
            LocalDate.of(2028, 5, 2) to "부처님오신날",
            LocalDate.of(2028, 5, 5) to "어린이날",
            LocalDate.of(2028, 6, 6) to "현충일",
            LocalDate.of(2028, 8, 15) to "광복절",
            LocalDate.of(2028, 10, 2) to "추석 연휴",
            LocalDate.of(2028, 10, 3) to "추석 · 개천절",
            LocalDate.of(2028, 10, 4) to "추석 연휴",
            LocalDate.of(2028, 10, 5) to "대체공휴일 (개천절)",
            LocalDate.of(2028, 10, 9) to "한글날",
            LocalDate.of(2028, 12, 25) to "성탄절",
        ),
    )
}

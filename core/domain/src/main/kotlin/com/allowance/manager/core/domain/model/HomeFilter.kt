package com.allowance.manager.core.domain.model

/**
 * 홈 내역 리스트 필터 — 멀티 토글(메인 / 숨김 / 전체).
 *
 * - 메인(showMain): 메인 계좌·수동 내역 포함
 * - 숨김(showHidden): 숨김(무시) 처리한 내역 포함 — 복원 진입점
 * - 전체: 두 토글이 모두 꺼진 상태(isAll) — 감지된 전부 표시
 *
 * 메인·숨김은 각각 독립 토글이라 둘 다 켤 수 있고, 둘 다 끄면 자동으로 '전체'가 된다.
 */
data class HomeFilter(
    val showMain: Boolean = true,
    val showHidden: Boolean = false,
) {
    /** 메인·숨김 모두 꺼짐 → 전체(필터 없음) */
    val isAll: Boolean get() = !showMain && !showHidden

    companion object {
        val Default = HomeFilter()   // 기본: 메인만
    }
}

/** 헤더에 노출하는 필터 칩 — 순서 고정(메인 → 숨김 → 전체) */
enum class HomeFilterChip(val label: String) {
    MAIN("메인"),
    HIDDEN("숨김"),
    ALL("전체"),
}

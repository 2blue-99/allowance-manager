package com.allowance.manager.core.domain.model

/**
 * 내역 리스트 필터 — 멀티 토글(메인 / 숨김 / 전체). 홈·월별 화면이 공유한다.
 *
 * - 메인(showMain): 메인 계좌·수동 내역 포함
 * - 숨김(showHidden): 숨김(무시) 처리한 내역 포함 — 복원 진입점
 * - 전체: 두 토글이 모두 꺼진 상태(isAll) — 감지된 전부 표시
 *
 * 메인·숨김은 각각 독립 토글이라 둘 다 켤 수 있고, 둘 다 끄면 자동으로 '전체'가 된다.
 * (저장은 화면별로 분리 — 홈 기본은 메인만, 월별 기본은 전체)
 */
data class LedgerFilter(
    val showMain: Boolean = true,
    val showHidden: Boolean = false,
) {
    /** 메인·숨김 모두 꺼짐 → 전체(필터 없음) */
    val isAll: Boolean get() = !showMain && !showHidden

    companion object {
        val Home = LedgerFilter(showMain = false, showHidden = false)     // 홈 기본: 전체 (이후 선택은 저장됨)
        val Calendar = LedgerFilter(showMain = false, showHidden = false) // 월별: 전체 고정
    }
}

/** 헤더에 노출하는 필터 칩 — 순서 고정(메인 → 숨김 → 전체) */
enum class LedgerFilterChip(val label: String) {
    MAIN("메인"),
    HIDDEN("숨김"),
    ALL("전체"),
}

/** 칩 탭 결과 — 전체는 배타(둘 다 해제), 메인·숨김은 각각 독립 토글 */
fun LedgerFilter.toggle(chip: LedgerFilterChip): LedgerFilter = when (chip) {
    LedgerFilterChip.MAIN -> copy(showMain = !showMain)
    LedgerFilterChip.HIDDEN -> copy(showHidden = !showHidden)
    LedgerFilterChip.ALL -> LedgerFilter(showMain = false, showHidden = false)
}

/** 내역 한 건이 현재 필터에 노출되어야 하는지 */
fun LedgerFilter.matches(tx: Transaction): Boolean = when {
    isAll -> true
    showMain && showHidden -> tx.isMain || tx.isManual || tx.isIgnored
    showMain -> (tx.isMain || tx.isManual) && !tx.isIgnored
    else -> tx.isIgnored   // 숨김만
}

package com.allowance.manager.core.domain.model

/**
 * 내역 리스트 필터 — 메인 / 전체 2단. 홈·월별 화면이 공유한다.
 *
 * - 메인(showMain): 가계부 집계 대상(BUDGET·LEDGER_ONLY = inLedger)만 표시
 * - 전체(isAll): showMain 이 꺼진 상태 — 감지된 전부 표시(EXCLUDED·미등록 포함)
 *
 * (showHidden 은 구 '숨김' 칩 잔재 — 항상 false. 저장 스키마 호환을 위해 필드만 남겨둠.)
 */
data class LedgerFilter(
    val showMain: Boolean = true,
    val showHidden: Boolean = false,
) {
    /** 메인 꺼짐 → 전체(필터 없음) */
    val isAll: Boolean get() = !showMain

    companion object {
        val Home = LedgerFilter(showMain = false)     // 홈 기본: 전체 (이후 선택은 저장됨)
        val Calendar = LedgerFilter(showMain = false)  // 월별: 전체 고정
    }
}

/** 헤더에 노출하는 필터 칩 — 순서 고정(메인 → 전체) */
enum class LedgerFilterChip(val label: String) {
    MAIN("메인"),
    ALL("전체"),
}

/** 칩 탭 결과 — 메인/전체 배타 선택(radio) */
fun LedgerFilter.toggle(chip: LedgerFilterChip): LedgerFilter = when (chip) {
    LedgerFilterChip.MAIN -> LedgerFilter(showMain = true)
    LedgerFilterChip.ALL -> LedgerFilter(showMain = false)
}

/** 내역 한 건이 현재 필터에 노출되어야 하는지 */
fun LedgerFilter.matches(tx: Transaction): Boolean =
    if (showMain) tx.inLedger else true   // 메인 = 가계부 집계(BUDGET+LEDGER_ONLY), 전체 = 전부

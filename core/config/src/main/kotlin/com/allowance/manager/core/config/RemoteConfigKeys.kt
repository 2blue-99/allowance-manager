package com.allowance.manager.core.config

object RemoteConfigKeys {
    const val FORCED_UPDATE_VERSION = "forced_update_version"
    const val UPDATE_NOTE = "update_note"

    /** 공휴일·은행 휴무일 목록 — 급여 지급일을 직전 영업일로 당길 때 사용. JSON 배열(["2026-01-01",…]) */
    const val KR_HOLIDAYS = "kr_holidays"
}

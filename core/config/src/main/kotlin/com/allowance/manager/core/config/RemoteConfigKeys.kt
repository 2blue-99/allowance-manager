package com.allowance.manager.core.config

object RemoteConfigKeys {
    /** 강제 업데이트 최소 버전 — 앱 버전이 이 값 미만이면 강제 업데이트 팝업(닫기 불가). 예: "1.2.0" */
    const val UPDATE_FORCED_VERSION = "update_forced_version"

    /** 추천 업데이트 최소 버전 — 앱 버전이 이 값 미만이면 추천 팝업(확인/업데이트, 하루 1회). 예: "1.2.0" */
    const val UPDATE_RECOMMEND_VERSION = "update_recommend_version"

    /** 공휴일·은행 휴무일 목록 — 급여 지급일을 직전 영업일로 당길 때 사용. JSON({version, holidays:[{date,name}]}) */
    const val KR_HOLIDAYS = "kr_holidays"

    /** 홈 진입 시 띄우는 공지 다이얼로그 — JSON({id, active, title, body}). active=false거나 비면 안 띄움. */
    const val NOTICE = "notice"
}

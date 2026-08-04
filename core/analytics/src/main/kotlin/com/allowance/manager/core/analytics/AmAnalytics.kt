package com.allowance.manager.core.analytics

/**
 * Analytics 이벤트·파라미터·User Property·다이얼로그 태그 상수 모음.
 * 문자열을 여기 한 곳에서만 정의하고 호출부는 참조로 쓴다(오타·일괄수정 방지).
 * 문서: Obsidian `Analytics 이벤트 v3.md`
 */
object AmAnalytics {

    /** 이벤트 이름 (screen_view는 AnalyticsHelper.logScreenView가 담당) */
    object Event {
        const val NAV_TAB_SELECT = "nav_tab_select"

        // 홈
        const val HOME_FILTER_SELECT = "home_filter_select"
        const val HOME_TX_SWIPE_HIDE = "home_tx_swipe_hide"
        const val HOME_TX_SWIPE_DELETE = "home_tx_swipe_delete"
        const val HOME_ADD_FAB_CLICK = "home_add_fab_click"
        const val HOME_TX_ITEM_CLICK = "home_tx_item_click"

        // 내역 저장(상세/추가 시트)
        const val TX_SAVE = "tx_save"
        const val TX_ADD_SAVE = "tx_add_save"

        // 월별
        const val CALENDAR_MONTH_CHANGE = "calendar_month_change"
        const val CALENDAR_SEARCH_TOGGLE = "calendar_search_toggle"
        const val CALENDAR_CATEGORY_FILTER = "calendar_category_filter"
        const val CALENDAR_TX_SWIPE_HIDE = "calendar_tx_swipe_hide"
        const val CALENDAR_TX_SWIPE_DELETE = "calendar_tx_swipe_delete"

        // 통계
        const val STATS_MONTH_SELECT = "stats_month_select"
        const val STATS_WINDOW_MOVE = "stats_window_move"

        // 설정
        const val SETTING_STATUSBAR_TOGGLE = "setting_statusbar_toggle"
        const val SETTING_ACCOUNT_MANAGE_CLICK = "setting_account_manage_click"
        const val SETTING_IGNORED_MANAGE_CLICK = "setting_ignored_manage_click"
        const val SETTING_DONATE_CLICK = "setting_donate_click"
        const val DONATE_RESULT = "donate_result"
        const val SETTING_FEEDBACK_CLICK = "setting_feedback_click"
        const val SETTING_RATE_CLICK = "setting_rate_click"

        // 계좌
        const val ACCOUNT_ADD = "account_add"
        const val ACCOUNT_ENABLED_TOGGLE = "account_enabled_toggle"
        const val ACCOUNT_DELETE = "account_delete"

        // 온보딩
        const val ONBOARDING_COMPLETE = "onboarding_complete"

        // 알림 자동기록 서비스 · 권한
        const val TX_AUTO_RECORDED = "tx_auto_recorded"
        const val TX_AUTO_IGNORED = "tx_auto_ignored"
        const val NOTI_PERMISSION_GRANTED = "noti_permission_granted"
        const val NOTI_PERMISSION_REVOKED = "noti_permission_revoked"

        // 다이얼로그 공통
        const val DIALOG_OPEN = "dialog_open"
        const val DIALOG_CONFIRM = "dialog_confirm"
        const val DIALOG_CANCEL = "dialog_cancel"
    }

    /** 이벤트 파라미터 키 */
    object Param {
        const val TAB = "tab"
        const val FILTER = "filter"
        const val HIDDEN = "hidden"
        const val CATEGORY = "category"
        const val TYPE = "type"
        const val IS_MAIN = "is_main"
        const val IS_HIDDEN = "is_hidden"
        const val IS_MANUAL = "is_manual"
        const val DIRECTION = "direction"
        const val ACTIVE = "active"
        const val ENABLED = "enabled"
        const val RESULT = "result"
        const val PAYDAY = "payday"
        const val BUDGET = "budget"
        const val BANK = "bank"
        const val DIALOG = "dialog"
        const val BY = "by"
    }

    /** User Property 키 */
    object UserProp {
        const val USER_TYPE = "user_type"
        const val MAIN_BANK = "main_bank"
        const val HAS_MAIN_ACCOUNT = "has_main_account"
        const val BUDGET_RANGE = "budget_range"
        const val PAYDAY = "payday"
        const val STATUSBAR_ENABLED = "statusbar_enabled"
    }

    /** 다이얼로그 태그(=dialog 파라미터 값) */
    object Dialog {
        const val IGNORE = "ignore"
        const val IGNORE_REMOVE = "ignore_remove"
        const val BUDGET = "budget"
        const val PAYDAY = "payday"
        const val USERTYPE = "usertype"
        const val ACCOUNT_EDIT = "account_edit"
        const val FORCE_UPDATE = "force_update"
        const val DONATE_THANKS = "donate_thanks"
    }
}

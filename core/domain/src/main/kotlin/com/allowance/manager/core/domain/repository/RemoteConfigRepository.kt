package com.allowance.manager.core.domain.repository

import com.allowance.manager.core.domain.model.Announcement
import com.allowance.manager.core.domain.model.Holidays

interface RemoteConfigRepository {
    suspend fun fetchAndActivate(): Boolean

    /** 강제 업데이트 최소 버전. 앱 버전이 이 값 미만이면 강제 팝업. 비면 강제 없음. */
    fun getForcedUpdateVersion(): String

    /** 추천 업데이트 최소 버전. 앱 버전이 이 값 미만이면 추천 팝업. 비면 추천 없음. */
    fun getRecommendUpdateVersion(): String

    /** 공휴일·은행 휴무일 목록 (급여 지급일 보정용). 원격 값이 없으면 내장 폴백(KrHolidays.FALLBACK). */
    fun getHolidays(): Holidays

    /** 홈 공지 다이얼로그 내용. 값이 없거나 active=false면 null. */
    fun getAnnouncement(): Announcement?
}

package com.allowance.manager.core.domain.repository

import com.allowance.manager.core.domain.model.Holidays

interface RemoteConfigRepository {
    suspend fun fetchAndActivate(): Boolean
    fun getForcedUpdateVersion(): String
    fun getUpdateNote(): String

    /** 공휴일·은행 휴무일 목록 (급여 지급일 보정용). 원격 값이 없으면 내장 폴백(KrHolidays.FALLBACK). */
    fun getHolidays(): Holidays
}

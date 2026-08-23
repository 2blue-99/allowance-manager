package com.allowance.manager.core.domain.repository

import java.time.LocalDate

interface RemoteConfigRepository {
    suspend fun fetchAndActivate(): Boolean
    fun getForcedUpdateVersion(): String
    fun getUpdateNote(): String

    /** 공휴일·은행 휴무일 집합 (급여 지급일 보정용). 원격 값이 없으면 기본값(KrHolidays.FALLBACK). */
    fun getHolidays(): Set<LocalDate>
}

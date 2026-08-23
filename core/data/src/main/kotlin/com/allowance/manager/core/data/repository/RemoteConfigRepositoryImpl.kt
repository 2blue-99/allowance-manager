package com.allowance.manager.core.data.repository

import com.allowance.manager.core.config.RemoteConfigDataSource
import com.allowance.manager.core.config.RemoteConfigKeys
import com.allowance.manager.core.domain.model.KrHolidays
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigRepositoryImpl @Inject constructor(
    private val remoteConfigDataSource: RemoteConfigDataSource,
) : RemoteConfigRepository {

    override suspend fun fetchAndActivate(): Boolean =
        remoteConfigDataSource.fetchAndActivate()

    override fun getForcedUpdateVersion(): String =
        remoteConfigDataSource.getString(RemoteConfigKeys.FORCED_UPDATE_VERSION)

    override fun getUpdateNote(): String =
        remoteConfigDataSource.getString(RemoteConfigKeys.UPDATE_NOTE)

    // 원격 값이 비었거나 전부 파싱 실패면 기본 공휴일로 폴백 — 사이클 경계가 공백이 되는 일은 없어야 한다.
    override fun getHolidays(): Set<LocalDate> =
        KrHolidays.parse(remoteConfigDataSource.getString(RemoteConfigKeys.KR_HOLIDAYS))
            .ifEmpty { KrHolidays.FALLBACK }
}

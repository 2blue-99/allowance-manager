package com.allowance.manager.core.data.repository

import com.allowance.manager.core.config.RemoteConfigDataSource
import com.allowance.manager.core.config.RemoteConfigKeys
import com.allowance.manager.core.domain.model.Announcement
import com.allowance.manager.core.domain.model.Holidays
import com.allowance.manager.core.domain.model.KrHolidays
import com.allowance.manager.core.domain.repository.RemoteConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigRepositoryImpl @Inject constructor(
    private val remoteConfigDataSource: RemoteConfigDataSource,
) : RemoteConfigRepository {

    override suspend fun fetchAndActivate(): Boolean =
        remoteConfigDataSource.fetchAndActivate()

    override fun getForcedUpdateVersion(): String =
        remoteConfigDataSource.getString(RemoteConfigKeys.UPDATE_FORCED_VERSION)

    override fun getRecommendUpdateVersion(): String =
        remoteConfigDataSource.getString(RemoteConfigKeys.UPDATE_RECOMMEND_VERSION)

    // 파싱 결과 캐시 — getHolidays()는 사이클을 계산하는 Flow 안에서 매 emit마다 불린다.
    // 원격 문자열이 그대로면 재파싱하지 않는다. (JSON 파싱을 거래 한 건 바뀔 때마다 반복하면 낭비)
    private var cachedRaw: String? = null
    private var cached: Holidays = Holidays.EMPTY

    // 원격 값이 비었거나 파싱에 실패하면 내장 폴백 — 사이클 경계가 공백이 되는 일은 없어야 한다.
    @Synchronized
    override fun getHolidays(): Holidays {
        val raw = remoteConfigDataSource.getString(RemoteConfigKeys.KR_HOLIDAYS)
        if (raw != cachedRaw) {
            cachedRaw = raw
            cached = KrHolidays.parse(raw).takeIf { it.count > 0 } ?: KrHolidays.FALLBACK
        }
        return cached
    }

    // 값이 없거나 active=false거나 파싱 실패면 null → 호출부가 다이얼로그를 안 띄운다.
    override fun getAnnouncement(): Announcement? =
        Announcement.parse(remoteConfigDataSource.getString(RemoteConfigKeys.NOTICE))
}

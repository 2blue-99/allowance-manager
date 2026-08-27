package com.allowance.manager.core.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigDataSource @Inject constructor(
    private val remoteConfig
    : FirebaseRemoteConfig,
) {
    suspend fun fetchAndActivate(): Boolean {
        val activated = remoteConfig.fetchAndActivate().await()
        logAll(activated)
        return activated
    }

    fun getString(key: String): String = remoteConfig.getString(key)

    fun getBoolean(key: String): Boolean = remoteConfig.getBoolean(key)

    fun getLong(key: String): Long = remoteConfig.getLong(key)

    fun getDouble(key: String): Double = remoteConfig.getDouble(key)

    // 받은 모든 키/값을 덤프 — 원격이 실제로 붙었는지·무슨 값이 내려왔는지 debug 로그로 확인.
    // 값이 길면(공휴일 JSON 등) 앞부분만 자른다. source: REMOTE=원격 / DEFAULT=기본값 / STATIC=미수신.
    private fun logAll(activated: Boolean) {
        val all = remoteConfig.all
        Timber.tag(TAG).d("fetchAndActivate → activated=%s, keys=%d", activated, all.size)
        all.toSortedMap().forEach { (key, value) ->
            val source = when (value.source) {
                FirebaseRemoteConfig.VALUE_SOURCE_REMOTE -> "REMOTE"
                FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT -> "DEFAULT"
                else -> "STATIC"
            }
            val raw = value.asString()
            val shown = if (raw.length > MAX_LOG) raw.take(MAX_LOG) + "…(${raw.length}자)" else raw
            Timber.tag(TAG).d("[%s] %s = %s", source, key, shown)
        }
    }

    private companion object {
        const val TAG = "RemoteConfig"
        const val MAX_LOG = 300
    }
}

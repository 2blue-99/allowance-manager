package com.allowance.manager.core.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigDataSource @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
) {
    suspend fun fetchAndActivate(): Boolean = remoteConfig.fetchAndActivate().await()

    fun getString(key: String): String = remoteConfig.getString(key)

    fun getBoolean(key: String): Boolean = remoteConfig.getBoolean(key)

    fun getLong(key: String): Long = remoteConfig.getLong(key)

    fun getDouble(key: String): Double = remoteConfig.getDouble(key)
}

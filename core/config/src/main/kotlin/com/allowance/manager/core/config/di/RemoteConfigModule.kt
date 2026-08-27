package com.allowance.manager.core.config.di

import com.allowance.manager.core.config.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigModule {

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig = Firebase.remoteConfig.apply {
        setConfigSettingsAsync(
            remoteConfigSettings {
                // debug은 값 확인이 잦으니 1초, release는 과도한 조회를 막아 10분(600초).
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 1 else 600
            }
        )
    }
}

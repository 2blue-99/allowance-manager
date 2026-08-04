package com.allowance.manager.core.analytics.di

import com.allowance.manager.core.analytics.AnalyticsHelper
import com.allowance.manager.core.analytics.FirebaseAnalyticsHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsHelper(impl: FirebaseAnalyticsHelper): AnalyticsHelper
}

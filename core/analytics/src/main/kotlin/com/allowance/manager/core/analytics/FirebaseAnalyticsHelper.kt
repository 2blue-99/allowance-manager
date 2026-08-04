package com.allowance.manager.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase 구현. 디버그 빌드에서는 Analytics/Crashlytics 수집을 끈다(개발 데이터 오염 방지).
 */
@Singleton
class FirebaseAnalyticsHelper @Inject constructor(
    @ApplicationContext context: Context,
) : AnalyticsHelper {

    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    init {
        val enabled = !BuildConfig.DEBUG
        analytics.setAnalyticsCollectionEnabled(enabled)
        crashlytics.isCrashlyticsCollectionEnabled = enabled
    }

    override fun logScreenView(screenName: String) {
        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply { putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName) },
        )
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                null -> Unit
                is String -> bundle.putString(key, value)
                is Boolean -> bundle.putString(key, value.toString())
                is Int -> bundle.putLong(key, value.toLong())
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                else -> bundle.putString(key, value.toString())
            }
        }
        analytics.logEvent(name, bundle)
    }

    override fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
    }

    override fun recordNonFatal(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }
}

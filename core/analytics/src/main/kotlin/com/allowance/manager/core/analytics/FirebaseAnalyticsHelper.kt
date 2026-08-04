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
        // Analytics는 디버그에서도 수집 → DebugView로 실시간 확인 가능
        // (디버그 모드 기기의 이벤트는 운영 리포트에서 제외됨)
        analytics.setAnalyticsCollectionEnabled(true)
        // Crashlytics는 디버그에서 끔 (개발 중 크래시가 대시보드를 오염시키지 않게)
        crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
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

package com.allowance.manager.feature.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.annotation.DrawableRes

/**
 * 홈 화면 위젯 고정(pin) 요청 헬퍼.
 *
 * 대표 위젯은 2x2 게이지([RemainWidget]) — 온보딩 이후 홈 가이드에서 "위젯 추가하기"로 유도한다.
 * 런처가 고정 요청을 지원하지 않으면([isPinSupported] == false) 호출부에서 안내 문구로 폴백한다.
 */
object WidgetPinner {

    /** 가이드 위젯 스텝에서 노출하는 대표 위젯 미리보기 이미지. */
    @get:DrawableRes
    val remainPreview: Int get() = R.drawable.preview_remain

    /** 런처가 위젯 고정 요청을 지원하는지. (API 26+ 에서만 유효, minSdk 26이므로 항상 호출 가능) */
    fun isPinSupported(context: Context): Boolean =
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /** 대표 위젯(RemainWidget) 홈 화면 고정 요청 → 시스템 팝업 노출. 미지원 런처면 무시된다. */
    fun requestPinRemainWidget(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return
        val provider = ComponentName(context, RemainWidgetReceiver::class.java)
        manager.requestPinAppWidget(provider, null, null)
    }
}

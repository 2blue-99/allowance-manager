package com.allowance.manager.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * 추가된 모든 잔액 위젯을 갱신. glance 의존을 위젯 모듈 안에 캡슐화한다.
 * (위젯 미추가 시 no-op)
 */
suspend fun refreshBalanceWidget(context: Context) {
    BalanceWidget().updateAll(context)
}

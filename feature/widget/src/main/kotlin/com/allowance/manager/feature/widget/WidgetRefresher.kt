package com.allowance.manager.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * 추가된 모든 위젯(4종)을 갱신. glance 의존을 위젯 모듈 안에 캡슐화한다.
 * 거래 저장·무시·삭제·예산/수급일 변경 등 데이터 변화 시 한곳에서 호출. (위젯 미추가 시 no-op)
 */
suspend fun refreshBalanceWidget(context: Context) {
    RemainWidget().updateAll(context)
    BalanceWidget().updateAll(context)
    DailyCoachWidget().updateAll(context)
    LedgerWidget().updateAll(context)
}

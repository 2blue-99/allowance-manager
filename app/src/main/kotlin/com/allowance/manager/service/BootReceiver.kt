package com.allowance.manager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 재부팅 후 상태바 서비스를 재시작한다.
 * (부팅 시점 포그라운드 시작 제한이 있을 수 있어 best-effort — 실패해도 앱 재실행 시 시작됨)
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            StatusBarService.start(context)
        }
    }
}

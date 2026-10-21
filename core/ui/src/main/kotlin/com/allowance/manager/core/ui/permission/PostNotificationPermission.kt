package com.allowance.manager.core.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * 알림 표시(POST_NOTIFICATIONS) 권한 — Android 13(TIRAMISU)+ 런타임 권한, 그 이하는 항상 허용.
 * 상태바 알림·예산 알림 등 앱의 알림은 이 권한이 없으면 **조용히 안 보인다** → 온보딩·설정 토글에서 요청한다.
 */
fun isPostNotificationGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** 시스템 '앱 알림 설정' 화면 — 영구 거부 후 사용자가 직접 켤 수 있게 안내할 때 */
fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/** 요청 결과 */
enum class PostNotificationResult {
    GRANTED,
    /** 거부 — 다시 물어볼 수 있음 */
    DENIED,
    /** 영구 거부("다시 묻지 않음") — 시스템 설정에서만 켤 수 있음 */
    DENIED_PERMANENTLY,
}

class PostNotificationPermissionState internal constructor(
    granted: Boolean,
    private val launch: ((PostNotificationResult) -> Unit) -> Unit,
) {
    var isGranted: Boolean by mutableStateOf(granted)
        internal set

    /** 권한이 없으면 시스템 다이얼로그를 띄우고 결과를 돌려준다. 이미 있으면 즉시 GRANTED */
    fun request(onResult: (PostNotificationResult) -> Unit) {
        if (isGranted) onResult(PostNotificationResult.GRANTED) else launch(onResult)
    }
}

/**
 * 컴포저블에서 쓰는 권한 상태 + 요청기. 화면 복귀(ON_RESUME) 시 자동 재조회 → 설정에서 켜고 돌아오면 반영.
 */
@Composable
fun rememberPostNotificationPermission(): PostNotificationPermissionState {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<((PostNotificationResult) -> Unit)?>(null) }
    val state = remember { mutableStateOf<PostNotificationPermissionState?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state.value?.isGranted = granted
        val result = when {
            granted -> PostNotificationResult.GRANTED
            // 거부 직후 rationale 이 false 면 "다시 묻지 않음" 상태 (또는 두 번째 거부)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (context as? Activity)?.let {
                    !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
                } == true -> PostNotificationResult.DENIED_PERMANENTLY
            else -> PostNotificationResult.DENIED
        }
        pending?.invoke(result)
        pending = null
    }

    val permission = state.value ?: PostNotificationPermissionState(
        granted = isPostNotificationGranted(context),
        launch = { onResult ->
            pending = onResult
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
    ).also { state.value = it }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permission.isGranted = isPostNotificationGranted(context)
    }
    return permission
}

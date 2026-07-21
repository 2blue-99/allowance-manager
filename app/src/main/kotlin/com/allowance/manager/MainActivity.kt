package com.allowance.manager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.allowance.manager.core.designsystem.CommonDialog
import com.allowance.manager.core.ui.theme.AllowanceManagerTheme
import com.allowance.manager.navigation.AppNavHost
import com.allowance.manager.service.StatusBarService
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val postNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onPostNotificationRequested()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 라이트 모드 전용 앱: 시스템 다크 모드와 무관하게 상태바/내비바 아이콘을 어둡게 고정.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> Timber.tag("FCM").d("토큰: $token") }
            .addOnFailureListener { e -> Timber.tag("FCM").e(e, "토큰 조회 실패") }
        setContent {
            AllowanceManagerTheme {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.shouldRequestPostNotification) {
                    if (uiState.shouldRequestPostNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val statusBarEnabled by viewModel.statusBarEnabled.collectAsState()
                LaunchedEffect(statusBarEnabled) {
                    if (statusBarEnabled) StatusBarService.start(this@MainActivity)
                }

                AppNavHost(navController = navController)

                if (uiState.showForceUpdateDialog) {
                    CommonDialog(
                        message = uiState.updateNote,
                        primaryButtonText = "업데이트",
                        onPrimaryClick = {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=$packageName"),
                                )
                            )
                        },
                        secondaryButtonText = "닫기",
                        onSecondaryClick = {},
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 상태바 표시용 POST_NOTIFICATIONS만 확인. 알림 접근(리스너) 유도는 온보딩에서만.
        val isPostNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        viewModel.onPostNotificationChecked(isPostNotificationGranted)
    }
}

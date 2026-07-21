package com.allowance.manager

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.allowance.manager.core.designsystem.CommonDialog
import com.allowance.manager.core.designsystem.theme.AllowanceManagerTheme
import com.allowance.manager.navigation.AppNavHost
import com.allowance.manager.service.StatusBarService
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

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
}

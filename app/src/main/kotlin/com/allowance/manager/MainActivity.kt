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
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.CompositionLocalProvider
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.AnalyticsHelper
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.analytics.LocalAnalyticsHelper
import com.allowance.manager.core.designsystem.theme.AllowanceManagerTheme
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.navigation.AppNavHost
import com.allowance.manager.service.StatusBarService
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @javax.inject.Inject
    lateinit var analyticsHelper: AnalyticsHelper

    override fun onResume() {
        super.onResume()
        // 앱을 볼 때마다 "본 시각" 갱신 → 가계부 관리 알림이 이미 확인한 내역엔 안 울리게
        viewModel.markAppSeen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // 시작 화면 판정(온보딩 여부 등) 전까지 시스템 스플래시를 유지
        splashScreen.setKeepOnScreenCondition { viewModel.startDestination.value == null }
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
                val startDestination by viewModel.startDestination.collectAsState()

                val statusBarEnabled by viewModel.statusBarEnabled.collectAsState()
                LaunchedEffect(statusBarEnabled) {
                    if (statusBarEnabled) StatusBarService.start(this@MainActivity)
                }

                // 판정이 끝나면(스플래시 해제) 결정된 시작지점에서 NavHost 구성
                startDestination?.let { start ->
                    CompositionLocalProvider(LocalAnalyticsHelper provides analyticsHelper) {
                    val navController = rememberNavController()
                    val uiState by viewModel.uiState.collectAsState()

                    AppNavHost(
                        navController = navController,
                        startDestination = start,
                        analytics = analyticsHelper,
                    )

                    if (uiState.showForceUpdateDialog) {
                        AmDialog(
                            title = "업데이트 안내",
                            // 강제 업데이트 → 닫기/뒤로로 닫히지 않음(무동작), 업데이트만 진행
                            onDismiss = {},
                            onConfirm = {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=$packageName"),
                                    )
                                )
                            },
                            confirmText = "업데이트",
                            dismissText = "닫기",
                            analyticsTag = AmAnalytics.Dialog.FORCE_UPDATE,
                        ) {
                            Text(uiState.updateNote, style = AmType.size14_medium, color = AmColors.TextSecondary)
                        }
                    }
                    }
                }
            }
        }
    }
}

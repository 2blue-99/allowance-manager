package com.allowance.manager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.compose.rememberNavController
import com.allowance.manager.core.designsystem.CommonDialog
import com.allowance.manager.core.ui.theme.AllowanceManagerTheme
import com.allowance.manager.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AllowanceManagerTheme {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsState()

                AppNavHost(navController = navController)

                if (uiState.showNotificationPermissionDialog) {
                    CommonDialog(
                        message = "카드·은행 결제 알림을 자동으로 감지하려면 알림 접근 권한이 필요합니다.\n설정에서 허용해주세요.",
                        primaryButtonText = "설정으로 이동",
                        onPrimaryClick = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        secondaryButtonText = "닫기",
                        onSecondaryClick = { viewModel.dismissNotificationPermissionDialog() },
                    )
                }

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
        val isGranted = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
        viewModel.onNotificationPermissionChecked(isGranted)
    }
}

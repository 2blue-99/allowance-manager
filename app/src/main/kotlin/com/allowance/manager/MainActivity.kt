package com.allowance.manager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.compose.rememberNavController
import com.allowance.manager.core.designsystem.CommonDialog
import com.allowance.manager.core.ui.theme.AllowanceManagerTheme
import com.allowance.manager.navigation.AppNavHost
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
        enableEdgeToEdge()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Timber.e("FCM Token: $token")
        }
        setContent {
            AllowanceManagerTheme {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.shouldRequestPostNotification) {
                    if (uiState.shouldRequestPostNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                AppNavHost(navController = navController)

                if (uiState.showNotificationListenerDialog) {
                    CommonDialog(
                        message = "카드·은행 결제 알림을 자동으로 감지하려면 알림 접근 권한이 필요합니다.\n설정에서 허용해주세요.",
                        primaryButtonText = "설정으로 이동",
                        onPrimaryClick = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        secondaryButtonText = "닫기",
                        onSecondaryClick = { viewModel.dismissNotificationListenerDialog() },
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
        val isListenerGranted = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
        val isPostNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        viewModel.onPermissionsChecked(isListenerGranted, isPostNotificationGranted)
    }
}

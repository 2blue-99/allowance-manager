package com.allowance.manager.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allowance.manager.core.designsystem.component.AmAppLogo
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmSpacing
import kotlinx.coroutines.delay

/** 상태 계층: ViewModel·네비게이션 의존성을 여기서 관리한다. */
@Composable
fun SplashRoute(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToIntro: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsState()

    LaunchedEffect(destination) {
        val dest = destination ?: return@LaunchedEffect
        delay(1000L)
        when (dest) {
            SplashDestination.INTRO -> onNavigateToIntro()
            SplashDestination.ONBOARDING -> onNavigateToOnboarding()
            SplashDestination.HOME -> onNavigateToHome()
        }
    }

    SplashScreen()
}

/** 표현 계층: 의존성 없이 화면만 그린다. (Preview 가능) */
@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(AmColors.CardBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AmAppLogo(modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(AmSpacing.lg))
        Text(
            text = "가계부",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AmColors.TextPrimary,
        )
        Spacer(Modifier.height(AmSpacing.xs))
        Text(
            text = "알림으로 자동 기록되는 용돈 관리",
            fontSize = 13.sp,
            color = AmColors.TextSecondary,
        )
    }
}

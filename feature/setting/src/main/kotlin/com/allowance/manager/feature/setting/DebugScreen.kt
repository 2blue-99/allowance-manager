package com.allowance.manager.feature.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.AmScreenHeader
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType

/**
 * 개발용 디버그 화면. debug 빌드에서 설정 맨 아래 진입점으로만 노출.
 * 인트로/온보딩 등 평소 진입하기 힘든 플로우로 바로 이동.
 */
@Composable
fun DebugRoute(
    onBack: () -> Unit,
    onNavigateToIntro: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: DebugViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    DebugScreen(
        onBack = onBack,
        onNavigateToIntro = onNavigateToIntro,
        onNavigateToOnboarding = onNavigateToOnboarding,
        onResetHomeGuide = {
            viewModel.resetHomeGuide()
            Toast.makeText(context, "홈 가이드 리셋 — 홈 재진입 시 다시 노출", Toast.LENGTH_SHORT).show()
        },
    )
}

@Composable
fun DebugScreen(
    onBack: () -> Unit = {},
    onNavigateToIntro: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onResetHomeGuide: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().background(AmColors.ScreenBg).padding(AmSpacing.xl),
    ) {
        AmScreenHeader(title = "디버그", onBack = onBack)
        Spacer(Modifier.height(AmSpacing.lg))
        Text("화면 이동", style = AmType.label, color = AmColors.TextSecondary)
        Spacer(Modifier.height(AmSpacing.md))

        AmButton(text = "인트로 화면으로", onClick = onNavigateToIntro, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(AmSpacing.md))
        AmButton(text = "온보딩 화면으로", onClick = onNavigateToOnboarding, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(AmSpacing.xl))
        Text("가이드", style = AmType.label, color = AmColors.TextSecondary)
        Spacer(Modifier.height(AmSpacing.md))
        AmButton(text = "홈 가이드 다시 보기", onClick = onResetHomeGuide, modifier = Modifier.fillMaxWidth())
    }
}

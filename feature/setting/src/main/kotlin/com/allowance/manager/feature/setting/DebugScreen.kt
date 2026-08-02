package com.allowance.manager.feature.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.AmScreenHeader
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType

/**
 * 개발용 디버그 화면. debug 빌드에서 설정 맨 아래 진입점으로만 노출.
 * 인트로/온보딩 등 평소 진입하기 힘든 플로우로 바로 이동 + 홈 가이드 on/off.
 */
@Composable
fun DebugRoute(
    onBack: () -> Unit,
    onNavigateToIntro: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: DebugViewModel = hiltViewModel(),
) {
    val homeGuideEnabled by viewModel.homeGuideEnabled.collectAsStateWithLifecycle()
    DebugScreen(
        onBack = onBack,
        onNavigateToIntro = onNavigateToIntro,
        onNavigateToOnboarding = onNavigateToOnboarding,
        homeGuideEnabled = homeGuideEnabled,
        onHomeGuideEnabledChange = viewModel::setHomeGuideEnabled,
    )
}

@Composable
fun DebugScreen(
    onBack: () -> Unit = {},
    onNavigateToIntro: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    homeGuideEnabled: Boolean = false,
    onHomeGuideEnabledChange: (Boolean) -> Unit = {},
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
        // ON = 홈 재진입 시 가이드 노출 / OFF = 노출 안 함
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("홈 가이드 표시", style = AmType.bodyStrong, color = AmColors.TextPrimary)
            Switch(checked = homeGuideEnabled, onCheckedChange = onHomeGuideEnabledChange)
        }
    }
}

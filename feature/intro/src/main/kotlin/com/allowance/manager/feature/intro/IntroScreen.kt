package com.allowance.manager.feature.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.AmTextButton
import com.allowance.manager.core.designsystem.theme.AmColors
import kotlinx.coroutines.launch

private val Accent = AmColors.Emerald
private val PlaceholderBg = AmColors.ChipBg
private val TextPrimary = AmColors.TextPrimary
private val TextSecondary = AmColors.TextSecondary

private data class IntroPage(val title: String, val description: String)

private val introPages = listOf(
    IntroPage("어느새 텅장…\n가계부는 또 안 썼네", "매번 적기 귀찮고,\n돈은 어디 갔는지 모르겠고"),
    IntroPage("그럴 때 필요한 건\n내돈지켜!!", "결제 알림을 자동으로 감지해\n불편함을 확 줄였다구~"),
    IntroPage("위젯으로\n자산을 카운팅!", "불필요한 소비를 막아줘~~"),
    IntroPage("가계부 기능도 튼튼하고", "통계도 제공한다구~~"),
    IntroPage("텅장 대신 통장으로!\n지금 시작해봐요!", ""),
)

@Composable
fun IntroRoute(
    onFinish: () -> Unit,
    viewModel: IntroViewModel = hiltViewModel(),
) {
    val isFinished by viewModel.isFinished.collectAsStateWithLifecycle()
    LaunchedEffect(isFinished) {
        if (isFinished) onFinish()
    }
    IntroScreen(onFinish = viewModel::finish)
}

@Composable
fun IntroScreen(onFinish: () -> Unit = {}) {
    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == introPages.size - 1

    // 좌우 패딩은 Column이 아니라 각 요소/페이저 contentPadding으로 처리해야
    // 페이저가 full-width로 동작하며 페이지가 잘리지 않고 부드럽게 넘어감.
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 굳이 필요없어보임
//            if (!isLast) {
//                AmTextButton("건너뛰기", onClick = onFinish, color = TextSecondary, fontWeight = FontWeight.Normal)
//            }
        }

        // 좌우 스와이프로 슬라이드 전환 (버튼으로도 이동 가능).
        // contentPadding으로 페이지를 인셋 → 옆 페이지가 살짝 보이며 자연스럽게 슬라이드.
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { index ->
            IntroSlide(introPages[index])
        }

        PageIndicator(current = pagerState.currentPage)
        Spacer(Modifier.height(16.dp))

        AmButton(
            text = if (isLast) "시작하기" else "다음",
            onClick = {
                if (isLast) onFinish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
    }
}

// 모든 슬라이드에서 이미지 높이를 동일하게 고정한다. (텍스트 줄 수와 무관)
private val IntroImageHeight = 260.dp

@Composable
private fun IntroSlide(page: IntroPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // TODO: 나중에 실제 이미지로 교체
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()

                .clip(RoundedCornerShape(16.dp))
                .background(PlaceholderBg),
            contentAlignment = Alignment.Center,
        ) {
            Text("이미지", color = TextSecondary, fontSize = 14.sp)
        }
        Spacer(Modifier.height(28.dp))
        Text(page.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, textAlign = TextAlign.Center)
        // 서브 문구가 있는 슬라이드만 표시 (마지막 CTA는 타이틀만)
        if (page.description.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(page.description, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
        // 남는 공간은 아래로 흘려보내 이미지·텍스트를 상단에 고정
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun PageIndicator(current: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(introPages.size) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == current) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (index == current) Accent else PlaceholderBg),
            )
        }
    }
}

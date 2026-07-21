package com.allowance.manager.feature.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import com.allowance.manager.core.designsystem.theme.AmColors
import kotlinx.coroutines.launch

private val Accent = AmColors.Emerald
private val PlaceholderBg = AmColors.ChipBg
private val TextPrimary = AmColors.TextPrimary
private val TextSecondary = AmColors.TextSecondary

private data class IntroPage(val title: String, val description: String)

private val introPages = listOf(
    IntroPage("매번 가계부 입력,\n귀찮았죠?", "이제 직접 안 적어도 돼요."),
    IntroPage("결제 알림을\n자동으로 감지해요", "카드·은행 알림만 있으면\n가계부가 저절로 채워져요."),
    IntroPage("이번달 예산과\n비교해요", "얼마 남았는지 위젯·상태바로\n한눈에 확인해요."),
    IntroPage("이제 시작해볼까요?", "알림 권한만 있으면 준비 끝!"),
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
        modifier = Modifier.fillMaxSize().background(Color.White).padding(vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (!isLast) {
                Text("건너뛰기", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.clickable(onClick = onFinish))
            } else {
                Spacer(Modifier.height(18.dp))
            }
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

        Button(
            onClick = {
                if (isLast) onFinish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        ) { Text(if (isLast) "시작하기" else "다음") }
    }
}

@Composable
private fun IntroSlide(page: IntroPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // TODO: 나중에 실제 이미지로 교체
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(PlaceholderBg),
            contentAlignment = Alignment.Center,
        ) {
            Text("이미지", color = TextSecondary, fontSize = 14.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(page.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(page.description, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
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

package com.allowance.manager.feature.intro

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Accent = AmColors.Emerald
private val IndicatorTrack = AmColors.ChipBg

// 온보딩 소개 웹툰 — 1~4는 두 컷을 세로로 합친 이미지, 5는 세로 CTA. (대사는 이미지에 포함 → 별도 텍스트 없음)
private val introPages = listOf(
    R.drawable.intro_page_1,
    R.drawable.intro_page_2,
    R.drawable.intro_page_3,
    R.drawable.intro_page_4,
    R.drawable.intro_page_5,
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

    // 진입 시 흰 배경만 보이다가 0.5초 뒤 콘텐츠를 페이드로 노출
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(500)
        contentVisible = true
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        AnimatedVisibility(visible = contentVisible, enter = fadeIn()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 상태바 아래 여백
                Spacer(Modifier.height(16.dp))

                // 좌우 스와이프로 슬라이드 전환. 이미지는 풀블리드(화면 폭 전체).
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) { index ->
                    IntroSlide(introPages[index])
                }

                Spacer(Modifier.height(12.dp))
                PageIndicator(current = pagerState.currentPage)
                Spacer(Modifier.height(16.dp))

                AmButton(
                    text = if (isLast) "시작하기" else "다음",
                    onClick = {
                        if (isLast) onFinish()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    textStyle = AmType.size14_bold,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun IntroSlide(@DrawableRes image: Int) {
    // 화면 끝에 붙지 않게 여백 + 둥근 모서리(카드 느낌). 이미지는 2:3 비율로 폭을 채운다.
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(24.dp)),
        )
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
                    .background(if (index == current) Accent else IndicatorTrack),
            )
        }
    }
}

package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.allowance.manager.core.designsystem.R

/**
 * 앱 로고 배지. 런처 아이콘과 같은 모습이라 스플래시 등에서 브랜드 연결감을 준다.
 * 배경(남색 라운드 사각형)을 품고 있어 밝은 화면 위에 그대로 올려도 된다.
 */
@Composable
fun AmAppLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.am_app_logo),
        contentDescription = null,
        modifier = modifier,
    )
}

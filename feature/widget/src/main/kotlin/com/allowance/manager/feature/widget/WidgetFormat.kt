package com.allowance.manager.feature.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.theme.AmColors

/**
 * 위젯 공용 색 (앱 라이트 팔레트 고정 — 위젯은 자체 색으로 렌더).
 */
internal object WidgetColors {
    val Navy = AmColors.Navy.toArgb()
    val Emerald = AmColors.Emerald.toArgb()
    val Red = AmColors.Red.toArgb()
    val Track = AmColors.BarTrack.toArgb()
}

/**
 * 금액 문자열 길이에 맞춰 폰트 크기를 단계 축소.
 * (Glance는 자동 축소가 없어 자릿수 기반으로 맞춘다. [com.allowance.manager.core.domain.util.toCompactWon]로
 * 이미 짧아진 문자열 기준.)
 *
 * @param base 기본(짧은 금액) sp
 */
internal fun moneySp(text: String, base: Int): TextUnit {
    val len = text.length
    return when {
        len <= 7 -> base.sp
        len == 8 -> (base - 3).sp
        len == 9 -> (base - 6).sp
        else -> (base - 8).sp
    }
}

/**
 * 결정형 링 게이지 비트맵. Glance엔 결정형 원형 진행바가 없어 Canvas로 직접 그린다.
 *
 * @param sizePx 정사각 비트맵 한 변(px)
 * @param progress 0f~1f 채움 비율 (12시 방향 시작, 시계방향)
 * @param over 예산 초과 — true면 링 전체를 빨강으로
 */
internal fun ringBitmap(sizePx: Int, progress: Float, over: Boolean): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val stroke = sizePx * 0.11f
    val pad = stroke / 2f + sizePx * 0.02f
    val rect = RectF(pad, pad, sizePx - pad, sizePx - pad)

    val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = WidgetColors.Track
    }
    canvas.drawArc(rect, 0f, 360f, false, track)

    val sweep = if (over) 360f else (progress.coerceIn(0f, 1f)) * 360f
    if (sweep > 0f) {
        val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            color = if (over) WidgetColors.Red else WidgetColors.Emerald
        }
        canvas.drawArc(rect, -90f, sweep, false, arc)
    }
    return bmp
}

package com.allowance.manager.core.designsystem.component

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * 숫자 입력을 천 단위 콤마로 표시하는 VisualTransformation.
 * 실제 저장값은 숫자만 유지하고 화면에만 "1,234,567" 형태로 보여준다.
 * (입력값은 숫자만 들어온다고 가정 — 호출부에서 filter { it.isDigit() } 처리)
 */
class AmThousandsTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }
        val n = digits.length

        val sb = StringBuilder()
        // 원본 i번째 숫자가 변환문자열에서 갖는 위치
        val origToTrans = IntArray(n + 1)
        for (i in 0 until n) {
            origToTrans[i] = sb.length
            sb.append(digits[i])
            val remaining = n - 1 - i
            if (remaining > 0 && remaining % 3 == 0) sb.append(',')
        }
        origToTrans[n] = sb.length
        val formatted = sb.toString()

        // 변환문자열 위치 t 이전의 숫자 개수 = 원본 위치
        val transToOrig = IntArray(formatted.length + 1)
        var count = 0
        for (t in formatted.indices) {
            if (formatted[t].isDigit()) count++
            transToOrig[t + 1] = count
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                origToTrans[offset.coerceIn(0, n)]

            override fun transformedToOriginal(offset: Int): Int =
                transToOrig[offset.coerceIn(0, formatted.length)]
        }

        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

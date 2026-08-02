package com.allowance.manager.core.ui.transaction

import androidx.compose.ui.graphics.Color
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.util.amountToComma
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** 메모 최대 글자 수 */
const val MEMO_MAX = 500

/** 부호 포함 금액 문자열. 수입·환불은 '+', 지출은 '-'. */
fun signedAmount(tx: Transaction): String {
    val magnitude = abs(tx.amount).amountToComma()
    return when {
        tx.type == TransactionType.INCOME -> "+${magnitude}원"
        tx.amount < 0 -> "+${magnitude}원"   // 취소·환불
        else -> "-${magnitude}원"
    }
}

/** 금액 색상. 흐림(무시·미등록) 우선, 그 외 수입·환불=초록, 지출=빨강. */
fun amountColor(tx: Transaction, dim: Boolean): Color = when {
    dim -> AmColors.TextSecondary
    tx.type == TransactionType.INCOME || tx.amount < 0 -> AmColors.Emerald
    else -> AmColors.Red
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)      // 오후 1:30
private val monthDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREAN)   // 08.02
private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yy.MM.dd", Locale.KOREAN) // 25.08.01
private val fullFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yy.MM.dd a h:mm", Locale.KOREAN) // 25.08.01 오후 1:30

/**
 * 리스트용 타임스탬프(상대 표기).
 * - 오늘: 시각만 (오후 1:30)
 * - 1년 이내(오늘 제외): 월.일 (08.02)
 * - 1년 초과: 연.월.일 (25.08.01)
 */
fun formatListTimestamp(epochMs: Long): String {
    val dateTime = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val date = dateTime.toLocalDate()
    val today = LocalDate.now()
    return when {
        date == today -> dateTime.format(timeFormatter)
        date.isAfter(today.minusYears(1)) -> dateTime.format(monthDayFormatter)
        else -> dateTime.format(shortDateFormatter)
    }
}

/** 상세시트용 전체 표기 (항상 연.월.일 + 시각): 25.08.01 오후 1:30 */
fun formatFullTimestamp(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDateTime().format(fullFormatter)

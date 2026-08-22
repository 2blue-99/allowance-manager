package com.allowance.manager.core.domain.util

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val hourMinuteFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)   // 오후 9:00

/** 시(0~23)·분(0~59) → "오전/오후 h:mm". (알림 시각 표기 등 공용) */
fun formatTimeOfDay(hour: Int, minute: Int): String =
    LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)).format(hourMinuteFormatter)

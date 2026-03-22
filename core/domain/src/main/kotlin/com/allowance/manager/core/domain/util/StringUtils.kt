package com.allowance.manager.core.domain.util

import java.text.DecimalFormat

fun Long.amountToComma(): String {
    val decimalFormat = DecimalFormat("#,###")
    val formattedString = decimalFormat.format(this)
    return formattedString
}
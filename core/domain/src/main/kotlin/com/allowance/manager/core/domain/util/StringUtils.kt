package com.allowance.manager.core.domain.util

import java.text.DecimalFormat

fun Long.amountToComma(): String {
    val decimalFormat = DecimalFormat("#,###")
    val formattedString = decimalFormat.format(this)
    return formattedString
}

/**
 * "1.2.3" 형식의 버전 문자열을 비교해 수신자가 [currentVersion]보다 최신인지 반환.
 * 파싱 불가능한 경우 false 반환.
 */
fun String.checkForceVersion(currentVersion: String): Boolean {
    val forceVersionSplit = toVersionParts() ?: return false
    val currentVersionSplit = currentVersion.toVersionParts() ?: return false
    return forceVersionSplit.zip(currentVersionSplit).firstNotNullOfOrNull { (fv, cv) ->
        when {
            fv > cv -> true
            fv < cv -> false
            else -> null
        }
    } ?: false
}

private fun String.toVersionParts(): List<Int>? =
    trim().split(".").map { it.toIntOrNull() ?: return null }
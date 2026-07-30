package com.allowance.manager.core.ui.transaction

import androidx.compose.ui.graphics.Color
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.domain.model.TransactionCategory

/** 카테고리 → 색 매핑 (통계 도넛·랭킹 등 공용). null = 미분류. */
fun categoryColor(category: TransactionCategory?): Color = when (category) {
    TransactionCategory.FOOD -> AmColors.CatFood
    TransactionCategory.CAFE -> AmColors.CatCafe
    TransactionCategory.TRANSPORT -> AmColors.CatTransport
    TransactionCategory.SHOPPING -> AmColors.CatShopping
    TransactionCategory.LIVING -> AmColors.CatLiving
    TransactionCategory.HEALTH -> AmColors.CatHealth
    TransactionCategory.CULTURE -> AmColors.CatCulture
    TransactionCategory.EDUCATION -> AmColors.CatEducation
    TransactionCategory.HOUSING -> AmColors.CatHousing
    TransactionCategory.INCOME -> AmColors.CatIncome
    TransactionCategory.TRANSFER -> AmColors.CatTransfer
    TransactionCategory.ETC -> AmColors.CatEtc
    null -> AmColors.CatNone
}

/** 카테고리 표시 이름. null = "미분류" */
fun categoryLabel(category: TransactionCategory?): String = category?.label ?: "미분류"

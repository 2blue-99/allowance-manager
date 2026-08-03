package com.allowance.manager.core.ui.transaction

import androidx.compose.ui.graphics.Color
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.domain.model.TransactionCategory

/** 카테고리 → 색 매핑 (통계 도넛·랭킹 등 공용). 미지정(null)은 기타로 취급. */
fun categoryColor(category: TransactionCategory?): Color = when (category) {
    TransactionCategory.FOOD -> AmColors.CatFood
    TransactionCategory.CAFE -> AmColors.CatCafe
    TransactionCategory.TRANSPORT -> AmColors.CatTransport
    TransactionCategory.SHOPPING -> AmColors.CatShopping
    TransactionCategory.FASHION -> AmColors.CatFashion
    TransactionCategory.LIVING -> AmColors.CatLiving
    TransactionCategory.HEALTH -> AmColors.CatHealth
    TransactionCategory.CULTURE -> AmColors.CatCulture
    TransactionCategory.EDUCATION -> AmColors.CatEducation
    TransactionCategory.EVENT -> AmColors.CatEvent
    TransactionCategory.HOUSING -> AmColors.CatHousing
    TransactionCategory.INCOME -> AmColors.CatIncome
    TransactionCategory.TRANSFER -> AmColors.CatTransfer
    TransactionCategory.ETC -> AmColors.CatEtc
    null -> AmColors.CatEtc   // 디폴트 = 기타
}

/** 카테고리 표시 이름. 미지정(null)은 "기타". */
fun categoryLabel(category: TransactionCategory?): String = category?.label ?: TransactionCategory.ETC.label

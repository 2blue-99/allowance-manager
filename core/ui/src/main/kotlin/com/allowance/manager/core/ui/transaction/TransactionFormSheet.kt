package com.allowance.manager.core.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.analytics.AmAnalytics
import com.allowance.manager.core.analytics.LocalAnalyticsHelper
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.designsystem.component.AmSecondaryButton
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.component.AmThousandsTransformation
import com.allowance.manager.core.designsystem.component.AmToggle
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.TxScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 내역 폼 시트 모드. 추가(빈 폼) / 수정(기존 내역 프리필). */
sealed interface TxFormMode {
    data object Add : TxFormMode
    data class Edit(val tx: Transaction) : TxFormMode
}

/**
 * 내역 추가·수정 공용 바텀시트. 홈 FAB(추가)·내역 탭(수정)에서 모두 사용한다.
 *
 * - 추가/수정은 **같은 폼**이고, 데이터가 채워졌는지(수정)와 관리 영역 노출만 다르다.
 * - 유형·금액·사용처·분류·메모는 로컬 상태로만 두고 '저장/추가'에서 실제 반영(취소 시 버려짐).
 * - **분류만 상단 아이콘에 즉시 반영**되고, 나머지는 저장 후에만 반영된다.
 * - 수정 모드에만 슬림 헤더(아이콘·시간·삭제)와 관리 토글(숨김·메인등록·무시)이 노출된다.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormSheet(
    mode: TxFormMode,
    onDismiss: () -> Unit,
    budgetName: String = "예산",   // 자산 호칭(용돈/생활비/예산) — 예산 반영 안내문에 사용
    // 추가 모드 콜백
    onAdd: (TransactionType, Long, String, TransactionCategory?, String, TxScope) -> Unit = { _, _, _, _, _, _ -> },
    // 수정 모드 콜백 (유형·금액·사용처·메모·분류)
    onSaveTransaction: (Long, TransactionType, Long, String, String, TransactionCategory?) -> Unit = { _, _, _, _, _, _ -> },
    onSetScope: (Long, TxScope) -> Unit = { _, _ -> },
    onDelete: (Long) -> Unit = {},
    onPromoteToMain: (Transaction) -> Unit = {},
    onSaved: () -> Unit = {},
    // 무시(출처/계좌 차단): 등록 + 과거 내역 소급 삭제. 비메인·자동감지 내역에만 노출.
    onIgnoreSource: (Transaction) -> Unit = {},
    countIgnorable: suspend (Transaction) -> Int = { 0 },
) {
    val editTx = (mode as? TxFormMode.Edit)?.tx
    // 항상 풀로 올라오게(부분 확장 금지)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val analytics = LocalAnalyticsHelper.current

    // 편집 로컬 상태(취소 시 버려짐, 저장 시 반영). 수정 모드는 기존 값, 추가 모드는 기본값.
    val stateKey = editTx?.id ?: 0L
    var type by remember(stateKey) { mutableStateOf(editTx?.type ?: TransactionType.EXPENSE) }
    var amountText by remember(stateKey) {
        mutableStateOf(editTx?.let { abs(it.amount).toString() } ?: "")
    }
    // 출처(은행/앱): 읽기전용 — 헤더 표시·사용처 힌트·저장조건에만 사용. 편집하는 건 사용처(merchant)뿐.
    val source = editTx?.sourceName.orEmpty()
    var merchant by remember(stateKey) { mutableStateOf(editTx?.merchant.orEmpty()) }
    var category by remember(stateKey) { mutableStateOf<TransactionCategory?>(editTx?.category ?: TransactionCategory.ETC) }
    var memo by remember(stateKey) { mutableStateOf(editTx?.memo.orEmpty()) }
    // 수입 기본은 '가계부만'(예산 미반영), 그 외는 예산 반영
    var txScope by remember(stateKey) { mutableStateOf(editTx?.scope ?: defaultScopeFor(TransactionType.EXPENSE)) }
    // 추가 모드에서 사용자가 직접 고르기 전까진 유형에 따라 기본 scope 자동 반영
    var scopeTouched by remember(stateKey) { mutableStateOf(false) }
    LaunchedEffect(type) {
        if (editTx == null && !scopeTouched) txScope = defaultScopeFor(type)
    }
    var promote by remember(stateKey) { mutableStateOf(editTx?.isMain ?: false) }
    var showDeleteConfirm by remember(stateKey) { mutableStateOf(false) }
    var showIgnoreConfirm by remember(stateKey) { mutableStateOf(false) }
    var ignoreCount by remember(stateKey) { mutableIntStateOf(0) }

    val amount = amountText.toLongOrNull() ?: 0L
    // 출처가 있으면(자동감지) 사용처 선택 / 출처가 비면(직접추가) 사용처 필수
    val canSubmit = amount > 0 && (source.isNotBlank() || merchant.isNotBlank())

    // 슬라이드 아웃 후 닫기
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AmColors.CardBg,
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.94f)) {
            // ── 스크롤 콘텐츠 (작은 화면 대응) ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                if (editTx == null) {
                    // 추가 모드: 슬림 헤더(➕ 아이콘 + 제목) — 수정 모드 헤더와 톤 통일
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(46.dp).clip(AmShape.card).background(AmColors.EmeraldBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                tint = AmColors.Emerald,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text("내역 추가", style = AmType.size14_bold, color = AmColors.TextPrimary)
                    }
                    Spacer(Modifier.height(20.dp))
                } else {
                    // 수정 모드: 슬림 헤더(아이콘 + 시간 + 삭제). 아이콘 이모지는 분류를 즉시 반영.
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(46.dp).clip(AmShape.card).background(AmColors.EmeraldBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            // 이체면 💸, 아니면 분류 이모지(즉시 반영), 분류 없으면 수입/지출 기본
                            val headerEmoji = when {
                                type == TransactionType.TRANSFER -> "💸"
                                category != null -> category!!.emoji
                                editTx.type == TransactionType.INCOME || editTx.amount < 0 -> "💰"
                                else -> "💳"
                            }
                            Text(headerEmoji, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        // 출처(은행/앱) — 있으면 시간 위에 표시(읽기전용). 직접추가(출처 없음)면 시간만.
                        Column(modifier = Modifier.weight(1f)) {
                            if (source.isNotBlank()) {
                                Text(
                                    source,
                                    style = AmType.size14_bold,
                                    color = AmColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                formatFullTimestamp(editTx.createdAt),
                                style = AmType.size11_medium,
                                color = AmColors.TextSecondary,
                            )
                        }
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "삭제",
                            tint = AmColors.TextSecondary,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { showDeleteConfirm = true }
                                .padding(6.dp)
                                .size(22.dp),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // 유형(지출/수입/이체) — 최상단. 가로 꽉 채움(각 weight 1). 이체는 지출·수입 어디에도 안 잡히는 중립 타입.
                // 선택 색은 홈 금액색 규칙과 통일: 지출=빨강 / 수입=메인(에메랄드) / 이체=중립 회색.
                // 주요 선택 컨트롤이라 일반 칩보다 살짝 높게(44dp) — 배경은 꽉 차고 라벨은 중앙 유지
                val typeChipModifier = Modifier.weight(1f).height(44.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmChip("지출", type == TransactionType.EXPENSE, selectedColor = AmColors.Red, modifier = typeChipModifier) { type = TransactionType.EXPENSE }
                    AmChip("수입", type == TransactionType.INCOME, selectedColor = AmColors.Emerald, modifier = typeChipModifier) { type = TransactionType.INCOME }
                    AmChip("이체", type == TransactionType.TRANSFER, selectedColor = AmColors.TextSecondary, modifier = typeChipModifier) { type = TransactionType.TRANSFER }
                }

                // 관리 카드(유형 아래) — 예산 반영(추가·수정 공통) + 메인등록·무시(수정 전용)
                // 이체는 어떤 집계에도 안 잡혀 예산 반영 선택이 의미 없음 → 숨김(애니메이션)
                val showScope = type != TransactionType.TRANSFER
                // 수동 입력은 이미 집계 대상 → '메인 계좌로 등록' 불필요. 추가 모드엔 없음
                val showPromote = editTx != null && !editTx.isManual
                AnimatedVisibility(visible = showScope || showPromote) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 유형과의 간격 (카드와 함께 접힘)
                        Spacer(Modifier.height(16.dp))
                        AmCard(
                            modifier = Modifier.fillMaxWidth(),
                            color = AmColors.ScreenBg,
                            contentPadding = PaddingValues(horizontal = AmSpacing.lg, vertical = AmSpacing.xs),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                AnimatedVisibility(visible = showScope) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        SheetScopeSelector(
                                            scope = txScope,
                                            budgetName = budgetName,
                                            onScope = { txScope = it; if (editTx == null) scopeTouched = true },
                                        )
                                        // 아래 토글이 함께 있을 때만 구분선 (scope와 함께 접힘)
                                        if (showPromote) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 5.dp)
                                                    .height(1.5.dp)
                                                    .background(AmColors.BarTrack),
                                            )
                                        }
                                    }
                                }
                                if (editTx != null && !editTx.isManual) {
                                    SheetToggleRow(
                                        title = "메인 계좌로 등록",
                                        // 계좌번호가 있으면 번호를, 없으면 출처(사용처)를 안내
                                        subtitle = editTx.extractedAccount?.takeIf { it.isNotBlank() } ?: source,
                                        checked = promote,
                                        onCheckedChange = { promote = it },
                                    )
                                }
                            }
                        }
                    }
                }

                // 무시(출처/계좌 차단) — 비메인·자동감지 내역에만. 숨김과 달리 알림 자체를 안 받음.
                if (editTx != null && !editTx.isMain && !editTx.isManual) {
                    Spacer(Modifier.height(14.dp))
                    IgnoreSourceButton(
                        label = if (!editTx.extractedAccount.isNullOrBlank()) "해당 계좌 알림 무시하기" else "해당 출처 알림 무시하기",
                        onClick = { scope.launch { ignoreCount = countIgnorable(editTx); showIgnoreConfirm = true } },
                    )
                }

                Spacer(Modifier.height(24.dp))
                SheetLabel("금액")
                Spacer(Modifier.height(10.dp))
                AmTextField(
                    value = amountText,
                    onValueChange = { v -> amountText = v.filter { it.isDigit() } },
                    label = "금액 (원)",
                    keyboardType = KeyboardType.Number,
                    // 저장값은 숫자만, 화면에는 천 단위 콤마로 표시
                    visualTransformation = AmThousandsTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))
                SheetLabel("사용처")
                Spacer(Modifier.height(10.dp))
                AmTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    // 출처(은행)는 헤더에 이미 보이므로, 사용처 칸은 빈 칸 느낌의 일반 힌트만.
                    label = "예: 스타벅스",
                    modifier = Modifier.fillMaxWidth(),
                )

                // 이체는 분류 개념이 없음 → 분류 섹션 숨김(저장 시 category=null)
                if (type != TransactionType.TRANSFER) {
                    Spacer(Modifier.height(24.dp))
                    SheetLabel("분류")
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransactionCategory.entries.forEach { cat ->
                            AmChip(
                                label = "${cat.emoji} ${cat.label}",
                                // 미지정은 기타로 취급 → 기타 칩이 기본 선택으로 보임
                                selected = (category ?: TransactionCategory.ETC) == cat,
                                // 선택 시 각 카테고리색으로 채움
                                selectedColor = categoryChipColor(cat),
                            ) { category = if (category == cat) null else cat }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                SheetLabel("메모")
                Spacer(Modifier.height(10.dp))
                AmTextField(
                    value = memo,
                    // 줄바꿈 허용 + 최대 500자
                    onValueChange = { if (it.length <= MEMO_MAX) memo = it },
                    label = "메모를 남겨보세요",
                    singleLine = false,
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))
            }

            // ── 하단 고정: 취소 / 추가·저장 ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AmSecondaryButton("취소", onClick = { close() }, modifier = Modifier.weight(1f))
                // 이체는 분류 없음 → 저장 시 category 강제 null
                val effectiveCategory = if (type == TransactionType.TRANSFER) null else category
                if (editTx == null) {
                    AmButton(
                        "추가",
                        onClick = { onAdd(type, amount, merchant, effectiveCategory, memo, txScope); close() },
                        modifier = Modifier.weight(1f),
                        enabled = canSubmit,
                    )
                } else {
                    AmButton(
                        "저장",
                        onClick = {
                            analytics.logEvent(
                                AmAnalytics.Event.TX_SAVE,
                                mapOf(
                                    AmAnalytics.Param.CATEGORY to (effectiveCategory ?: TransactionCategory.ETC).name,
                                    AmAnalytics.Param.TYPE to type.name.lowercase(),
                                    AmAnalytics.Param.IS_MAIN to promote,
                                    AmAnalytics.Param.IS_HIDDEN to (txScope == TxScope.EXCLUDED),
                                    AmAnalytics.Param.IS_MANUAL to editTx.isManual,
                                ),
                            )
                            // 실제로 바뀐 항목만 반영하고, 변경이 있을 때만 저장 알림
                            val memoChanged = memo.trim().ifBlank { null } != editTx.memo
                            val categoryChanged = effectiveCategory != editTx.category
                            val typeChanged = type != editTx.type
                            val amountChanged = amount != abs(editTx.amount)
                            val merchantChanged = merchant.trim().ifBlank { null } != editTx.merchant
                            val coreChanged = memoChanged || categoryChanged || typeChanged || amountChanged || merchantChanged
                            val scopeChanged = txScope != editTx.scope
                            val promoteNow = promote && !editTx.isMain
                            if (coreChanged) onSaveTransaction(editTx.id, type, amount, merchant, memo, effectiveCategory)
                            if (scopeChanged) onSetScope(editTx.id, txScope)
                            if (promoteNow) onPromoteToMain(editTx)
                            if (coreChanged || scopeChanged || promoteNow) onSaved()
                            close()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = canSubmit,
                    )
                }
            }
        }
    }

    // 삭제 확인 (되돌릴 수 없는 동작 → 확인 후 삭제)
    if (showDeleteConfirm && editTx != null) {
        AmDialog(
            title = "내역을 삭제할까요?",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = { showDeleteConfirm = false; onDelete(editTx.id); close() },
            confirmText = "삭제",
            confirmColor = AmColors.Red,
        ) {
            Text("삭제하면 되돌릴 수 없어요.", style = AmType.size14_medium, color = AmColors.TextSecondary)
        }
    }

    // 무시 확인 (되돌릴 수 없는 동작 → 확인 후 즉시 실행. 저장 버튼과 무관)
    if (showIgnoreConfirm && editTx != null) {
        val target = editTx.extractedAccount?.takeIf { it.isNotBlank() } ?: editTx.sourceName
        AmDialog(
            title = "이 알림을 무시할까요?",
            onDismiss = { showIgnoreConfirm = false },
            onConfirm = { showIgnoreConfirm = false; onIgnoreSource(editTx); close() },
            confirmText = "무시",
            confirmColor = AmColors.Red,
            analyticsTag = AmAnalytics.Dialog.IGNORE,
            analyticsParams = mapOf(AmAnalytics.Param.BY to if (!editTx.extractedAccount.isNullOrBlank()) "account" else "source"),
        ) {
            Text(
                "${target}에서 오는 알림을 앞으로 받지 않고, 기존 내역 ${ignoreCount}건도 모두 삭제돼요.",
                style = AmType.size14_medium,
                color = AmColors.TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "무시 해제는 설정에서 할 수 있어요.",
                style = AmType.size11_medium,
                color = AmColors.TextTertiary,
            )
        }
    }
}

/** 무시(출처/계좌 차단) 진입 버튼. 경고톤. 탭 → 확인 다이얼로그. */
@Composable
private fun IgnoreSourceButton(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AmShape.card)
            .background(AmColors.RedBg)
            .border(1.dp, AmColors.Red.copy(alpha = 0.28f), AmShape.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🚫", fontSize = 17.sp)
        Spacer(Modifier.width(11.dp))
        Text(label, style = AmType.size14_bold, color = AmColors.Red, modifier = Modifier.weight(1f))
        Text("›", style = AmType.size14_bold, color = AmColors.Red.copy(alpha = 0.5f))
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = AmType.size12_bold, color = AmColors.TextSecondary)
}

/** 유형별 기본 예산 반영 상태 — 수입은 '가계부만'(예산 미반영), 그 외는 예산 반영. */
private fun defaultScopeFor(type: TransactionType): TxScope =
    if (type == TransactionType.INCOME) TxScope.LEDGER_ONLY else TxScope.BUDGET

/** 예산 반영 상태 3분기 선택 (BUDGET / LEDGER_ONLY / EXCLUDED) */
@Composable
private fun SheetScopeSelector(scope: TxScope, budgetName: String, onScope: (TxScope) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        // 제목 없이 칩(호칭 반영)이 곧 라벨 역할 — 아래 안내문이 의미 보강
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AmChip("${budgetName} 반영", scope == TxScope.BUDGET, modifier = Modifier.weight(1f)) { onScope(TxScope.BUDGET) }
            AmChip("가계부만", scope == TxScope.LEDGER_ONLY, modifier = Modifier.weight(1f)) { onScope(TxScope.LEDGER_ONLY) }
            AmChip("제외", scope == TxScope.EXCLUDED, modifier = Modifier.weight(1f)) { onScope(TxScope.EXCLUDED) }
        }
        Spacer(Modifier.height(8.dp))
        // 선택한 상태별 안내 — 자산 호칭(용돈/생활비/예산)을 반영
        val hint = when (scope) {
            TxScope.BUDGET -> "${budgetName}에 포함됩니다."
            TxScope.LEDGER_ONLY -> "${budgetName}에는 포함되지 않고, 지출·수입에만 포함됩니다."
            TxScope.EXCLUDED -> "전체에서 제외됩니다."
        }
        Text(hint, style = AmType.size11_medium, color = AmColors.TextSecondary)
    }
}

@Composable
private fun SheetToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AmType.size14_bold, color = AmColors.TextPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = AmType.size11_medium, color = AmColors.TextSecondary)
            }
        }
        AmToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

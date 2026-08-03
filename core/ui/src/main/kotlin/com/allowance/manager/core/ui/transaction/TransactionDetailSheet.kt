package com.allowance.manager.core.ui.transaction

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.component.AmButton
import com.allowance.manager.core.designsystem.component.AmCard
import com.allowance.manager.core.designsystem.component.AmChip
import com.allowance.manager.core.designsystem.component.AmDialog
import com.allowance.manager.core.designsystem.component.AmSecondaryButton
import com.allowance.manager.core.designsystem.component.AmTextField
import com.allowance.manager.core.designsystem.component.AmToggle
import com.allowance.manager.core.designsystem.theme.AmColors
import com.allowance.manager.core.designsystem.theme.AmShape
import com.allowance.manager.core.designsystem.theme.AmSpacing
import com.allowance.manager.core.designsystem.theme.AmType
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import kotlinx.coroutines.launch

/**
 * 내역 상세·편집 바텀시트. 홈·월별 등 내역을 탭했을 때 공통으로 띄운다.
 *
 * - 숨김(합계 제외)·메인 계좌 등록 토글은 로컬 상태로만 두고 '저장'에서 실제 반영
 * - 분류/메모 수정도 저장 시 upsert
 * - 실제로 바뀐 항목이 있을 때만 [onSaved] 콜백 호출 (스낵바 등)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailSheet(
    tx: Transaction,
    onDismiss: () -> Unit,
    onSetHidden: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onPromoteToMain: (Transaction) -> Unit,
    onSaveTransaction: (Long, String, TransactionCategory?) -> Unit,
    onSaved: () -> Unit = {},
    // 무시(출처/계좌 차단): 등록 + 과거 내역 소급 삭제. 비메인·자동감지 내역에만 노출.
    onIgnoreSource: (Transaction) -> Unit = {},
    countIgnorable: suspend (Transaction) -> Int = { 0 },
) {
    // 항상 풀로 올라오게(부분 확장 금지)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // 편집 로컬 상태(취소 시 버려짐, 저장 시 upsert)
    var memo by remember(tx.id) { mutableStateOf(tx.memo.orEmpty()) }
    var category by remember(tx.id) { mutableStateOf(tx.category) }
    var hidden by remember(tx.id) { mutableStateOf(tx.isHidden) }
    var promote by remember(tx.id) { mutableStateOf(tx.isMain) }
    var showDeleteConfirm by remember(tx.id) { mutableStateOf(false) }
    var showIgnoreConfirm by remember(tx.id) { mutableStateOf(false) }
    var ignoreCount by remember(tx.id) { mutableIntStateOf(0) }

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
                // 식별 헤더: 아이콘 + 금액(주인공) + 은행·시간 + 삭제(휴지통, 금액 행 우측 정렬)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(AmShape.card).background(AmColors.EmeraldBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        val isIncome = tx.type == TransactionType.INCOME || tx.amount < 0
                        Text(category?.emoji ?: if (isIncome) "💰" else "💳", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(signedAmount(tx), style = AmType.amountLarge, color = amountColor(tx, false))
                        Text("${tx.sourceName} · ${formatFullTimestamp(tx.createdAt)}", style = AmType.caption, color = AmColors.TextSecondary)
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
                // 상단 관리 토글 묶음 (합계 제외 + 메인 계좌 등록)
                AmCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmColors.ScreenBg,
                    contentPadding = PaddingValues(horizontal = AmSpacing.lg, vertical = AmSpacing.xs),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SheetToggleRow(
                            title = "이번 달 합계에서 숨기기",
                            // 로컬 토글만 바꾸고 실제 반영은 저장 시
                            checked = hidden,
                            onCheckedChange = { hidden = it },
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .height(1.5.dp)
                                .background(AmColors.BarTrack),
                        )
                        SheetToggleRow(
                            title = "메인 계좌로 등록",
                            // 계좌번호가 있으면 번호를, 없으면 출처(앱 표시명)를 안내
                            subtitle = tx.extractedAccount?.takeIf { it.isNotBlank() } ?: tx.sourceName,
                            // 로컬 토글만 바꾸고(취소 가능) 실제 승격은 저장 시 반영
                            checked = promote,
                            onCheckedChange = { promote = it },
                        )
                    }
                }

                // 무시(출처/계좌 차단) — 비메인·자동감지 내역에만. 숨김과 달리 알림 자체를 안 받음.
                if (!tx.isMain && !tx.isManual) {
                    Spacer(Modifier.height(14.dp))
                    IgnoreSourceButton(
                        label = if (!tx.extractedAccount.isNullOrBlank()) "해당 계좌 알림 무시하기" else "해당 출처 알림 무시하기",
                        onClick = { scope.launch { ignoreCount = countIgnorable(tx); showIgnoreConfirm = true } },
                    )
                }

                Spacer(Modifier.height(24.dp))
                SheetLabel("분류")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionCategory.entries.forEach { cat ->
                        AmChip(
                            label = "${cat.emoji} ${cat.label}",
                            // 미지정은 기타로 취급 → 기타 칩이 기본 선택으로 보임
                            selected = (category ?: TransactionCategory.ETC) == cat,
                        ) { category = if (category == cat) null else cat }
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
                    // supportingText = "${memo.length}/$MEMO_MAX",  // 글자수 표시는 생략
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))
            }

            // ── 하단 고정: 취소 / 저장 ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AmSecondaryButton("취소", onClick = { close() }, modifier = Modifier.weight(1f))
                AmButton(
                    "저장",
                    onClick = {
                        // 실제로 바뀐 항목만 반영하고, 변경이 있을 때만 저장 알림
                        val memoChanged = memo.trim().ifBlank { null } != tx.memo
                        val categoryChanged = category != tx.category
                        val hiddenChanged = hidden != tx.isHidden
                        // 계좌번호 없어도 출처(앱) 기준으로 등록 가능 → extractedAccount 조건 제거
                        val promoteNow = promote && !tx.isMain
                        if (memoChanged || categoryChanged) onSaveTransaction(tx.id, memo, category)
                        if (hiddenChanged) onSetHidden(tx.id, hidden)
                        if (promoteNow) onPromoteToMain(tx)
                        if (memoChanged || categoryChanged || hiddenChanged || promoteNow) onSaved()
                        close()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // 삭제 확인 (되돌릴 수 없는 동작 → 확인 후 삭제)
    if (showDeleteConfirm) {
        AmDialog(
            title = "내역을 삭제할까요?",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = { showDeleteConfirm = false; onDelete(tx.id); close() },
            confirmText = "삭제",
            confirmColor = AmColors.Red,
        ) {
            Text("삭제하면 되돌릴 수 없어요.", style = AmType.body, color = AmColors.TextSecondary)
        }
    }

    // 무시 확인 (되돌릴 수 없는 동작 → 확인 후 즉시 실행. 저장 버튼과 무관)
    if (showIgnoreConfirm) {
        val target = tx.extractedAccount?.takeIf { it.isNotBlank() } ?: tx.sourceName
        AmDialog(
            title = "이 알림을 무시할까요?",
            onDismiss = { showIgnoreConfirm = false },
            onConfirm = { showIgnoreConfirm = false; onIgnoreSource(tx); close() },
            confirmText = "무시",
            confirmColor = AmColors.Red,
        ) {
            Text(
                "${target}에서 오는 알림을 앞으로 받지 않고, 기존 내역 ${ignoreCount}건도 모두 삭제돼요.",
                style = AmType.body,
                color = AmColors.TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "무시 해제는 설정에서 할 수 있어요.",
                style = AmType.caption,
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
        Text(label, style = AmType.bodyStrong, color = AmColors.Red, modifier = Modifier.weight(1f))
        Text("›", style = AmType.bodyStrong, color = AmColors.Red.copy(alpha = 0.5f))
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = AmType.label, color = AmColors.TextSecondary)
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
            Text(title, style = AmType.bodyStrong, color = AmColors.TextPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = AmType.caption, color = AmColors.TextSecondary)
            }
        }
        AmToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

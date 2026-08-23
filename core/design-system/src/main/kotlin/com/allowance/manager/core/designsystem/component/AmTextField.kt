package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * 공통 입력 필드. OutlinedTextField를 감싸 라벨/행수/키보드 타입을 표준화.
 * 여러 줄 입력은 singleLine=false + minLines 로, 글자수 표시는 supportingText 로.
 *
 * 한 줄 입력은 IME '완료' 버튼을 누르면 포커스를 해제해 키보드를 내린다(공통 동작).
 * 여러 줄 입력은 줄바꿈을 위해 이 동작을 적용하지 않는다.
 *
 * [supportingTextColor]는 "저장은 되지만 알려줄 게 있는" 안내에 쓴다(예: 지정한 날이 공휴일).
 * 진짜 에러가 아니므로 isError로 테두리까지 물들이지 않는다.
 */
@Composable
fun AmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    supportingText: String? = null,
    supportingTextColor: Color? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    // 컴팩트 모드: 라벨 대신 placeholder + 낮은 높이(검색창 등)
    dense: Boolean = false,
    // 한 줄 입력에서 IME '완료'를 눌렀을 때 추가 동작(예: 검색 확정 계측). 포커스 해제는 항상 수행.
    onImeAction: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    // 한 줄 입력만 '완료'로 포커스 해제 (여러 줄은 줄바꿈 유지)
    val imeAction = if (singleLine) ImeAction.Done else ImeAction.Default
    if (dense) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onImeAction(); focusManager.clearFocus() }),
            visualTransformation = visualTransformation,
            modifier = modifier.height(48.dp),
        )
        return
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeAction(); focusManager.clearFocus() }),
        visualTransformation = visualTransformation,
        supportingText = supportingText?.let {
            { if (supportingTextColor != null) Text(it, color = supportingTextColor) else Text(it) }
        },
        modifier = modifier,
    )
}

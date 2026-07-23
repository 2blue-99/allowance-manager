package com.allowance.manager.core.designsystem.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.allowance.manager.core.designsystem.theme.AmColors

/**
 * 토스식 하단 라인 텍스트필드. 박스 없이 밑줄만 표시하고, 입력 전엔 힌트를 노출.
 * 포커스 시 밑줄이 포인트 컬러로 바뀐다.
 */
@Composable
fun AmLineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(hint, fontSize = fontSize, color = AmColors.TextTertiary) },
        singleLine = true,
        textStyle = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Medium, color = AmColors.TextPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() },
            onGo = { onImeAction() },
            onSend = { onImeAction() },
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = AmColors.Emerald,
            unfocusedIndicatorColor = AmColors.BarTrack,
            cursorColor = AmColors.Emerald,
        ),
        modifier = modifier,
    )
}

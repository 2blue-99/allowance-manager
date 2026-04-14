package com.allowance.manager.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allowance.manager.core.domain.model.Spending
import com.allowance.manager.core.domain.util.amountToComma
import com.allowance.manager.core.ui.theme.AmStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeRoute(
    onNavigateToSetting: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onInputAmountChange = viewModel::onInputAmountChange,
        onSaveAmount = viewModel::saveAmount,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onInputAmountChange: (String) -> Unit = {},
    onSaveAmount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = uiState.balance,
            style = AmStyle.text20,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = uiState.inputAmount,
                onValueChange = onInputAmountChange,
                label = { Text("이번달 용돈") },
                suffix = { Text("원") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onSaveAmount) {
                Text("저장")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "소비 내역",
            style = AmStyle.text18,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.spendings.isEmpty()) {
            Text(
                text = "소비 내역이 없습니다",
                style = AmStyle.text14,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(
                    items = uiState.spendings,
                    key = { it.id },
                ) { spending ->
                    SpendingItem(spending = spending)
                    HorizontalDivider()
                }
            }
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")

@Composable
private fun SpendingItem(
    spending: Spending,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "${spending.amount.amountToComma()}원",
                style = AmStyle.text16,
            )
            Text(
                text = Instant.ofEpochMilli(spending.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(timeFormatter),
                style = AmStyle.text12,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "남은 잔액 ${spending.remainingBalance.amountToComma()}원",
            style = AmStyle.text14,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        uiState = HomeUiState(
            balance = "11,000",
            inputAmount = "50000",
            spendings = listOf(
                Spending(
                    id = 1,
                    amount = 3000,
                    remainingBalance = 47000,
                    timestamp = System.currentTimeMillis(),
                ),
                Spending(
                    id = 2,
                    amount = 15000,
                    remainingBalance = 32000,
                    timestamp = System.currentTimeMillis() - 3600_000,
                ),
            ),
        ),
    )
}

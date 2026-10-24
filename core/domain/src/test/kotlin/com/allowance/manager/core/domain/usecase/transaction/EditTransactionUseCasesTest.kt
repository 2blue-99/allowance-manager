package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionCategory
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.core.domain.usecase.InMemoryTransactionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시트 '추가' · '저장' 이 타는 UseCase — 부호 보존, 공백 정리, 직접 추가 표식 */
class EditTransactionUseCasesTest {

    private val transactions = InMemoryTransactionRepository()

    private suspend fun seed(type: TransactionType, amount: Long, scope: TxScope = TxScope.BUDGET): Long =
        transactions.record(
            Transaction(type = type, amount = amount, packageName = "com.bank", sourceName = "은행",
                merchant = "원래 가게", memo = "원래 메모", scope = scope, createdAt = 1_000L),
        )

    // ── UpdateTransactionUseCase ──

    @Test
    fun `환불(음수 지출)은 금액을 고쳐도 음수가 유지된다`() = runBlocking {
        val id = seed(TransactionType.EXPENSE, -5_600L)
        UpdateTransactionUseCase(transactions)(id, TransactionType.EXPENSE, 7_000L, "가게", "", null)
        assertEquals(-7_000L, transactions.getById(id)!!.amount)
    }

    @Test
    fun `환불을 수입으로 바꾸면 양수가 된다`() = runBlocking {
        val id = seed(TransactionType.EXPENSE, -5_600L)
        UpdateTransactionUseCase(transactions)(id, TransactionType.INCOME, 5_600L, "", "", null)
        val t = transactions.getById(id)!!
        assertEquals(TransactionType.INCOME, t.type)
        assertEquals(5_600L, t.amount)
    }

    @Test
    fun `사용처·메모는 공백을 정리하고 비면 null, 분류는 그대로 저장`() = runBlocking {
        val id = seed(TransactionType.EXPENSE, 5_600L)
        UpdateTransactionUseCase(transactions)(id, TransactionType.EXPENSE, 5_600L, "  스타벅스 ", "   ", TransactionCategory.CAFE)
        val t = transactions.getById(id)!!
        assertEquals("스타벅스", t.merchant)
        assertNull(t.memo)
        assertEquals(TransactionCategory.CAFE, t.category)
    }

    @Test
    fun `내용 수정은 계좌 매칭·예산 반영 상태를 건드리지 않는다`() = runBlocking {
        val id = transactions.record(
            Transaction(type = TransactionType.EXPENSE, amount = 1_000L, packageName = "", sourceName = "",
                accountId = 7L, scope = TxScope.LEDGER_ONLY, createdAt = 1_000L),
        )
        UpdateTransactionUseCase(transactions)(id, TransactionType.EXPENSE, 2_000L, "", "", null)
        val t = transactions.getById(id)!!
        assertEquals(7L, t.accountId)
        assertEquals(TxScope.LEDGER_ONLY, t.scope)
    }

    // ── AddManualTransactionUseCase ──

    @Test
    fun `직접 추가는 isManual, 출처 없음, 사용자가 고른 예산 반영 상태로 저장된다`() = runBlocking {
        val id = AddManualTransactionUseCase(transactions)(
            TransactionType.INCOME, 150_000L, " 용돈 ", TransactionCategory.INCOME, " ", TxScope.LEDGER_ONLY,
        )
        val t = transactions.getById(id)!!
        assertTrue(t.isManual)
        assertEquals("", t.sourceName)
        assertNull(t.accountId)
        assertEquals("용돈", t.merchant)
        assertNull(t.memo)
        assertEquals(TxScope.LEDGER_ONLY, t.scope)
        assertEquals(150_000L, t.amount)
    }

    // ── SetTxScopeUseCase ──

    @Test
    fun `예산 반영 상태 지정은 그 내역만 바꾼다`() = runBlocking {
        val a = seed(TransactionType.INCOME, 150_000L, TxScope.LEDGER_ONLY)
        val b = seed(TransactionType.INCOME, 20_000L, TxScope.LEDGER_ONLY)
        SetTxScopeUseCase(transactions)(a, TxScope.BUDGET)
        assertEquals(TxScope.BUDGET, transactions.getById(a)!!.scope)
        assertEquals(TxScope.LEDGER_ONLY, transactions.getById(b)!!.scope)
    }
}

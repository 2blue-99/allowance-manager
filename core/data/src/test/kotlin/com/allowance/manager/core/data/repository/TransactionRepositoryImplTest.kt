package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.core.local.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** 스와이프 제외/복원 — 복원 기본값은 기록 규칙과 같아야 한다 (지출·이체=BUDGET, 수입=LEDGER_ONLY) */
class TransactionRepositoryImplTest {

    private val dao = FakeTransactionDao()
    private val repo = TransactionRepositoryImpl(dao)

    private suspend fun seed(type: String, scope: String = "BUDGET"): Long = dao.insert(
        TransactionEntity(type = type, amount = 1_000L, balance = null, packageName = "p", sourceName = "s",
            extractedAccount = null, accountId = 1L, category = null, memo = null, scope = scope, createdAt = 1L),
    )

    @Test
    fun `제외하면 EXCLUDED`() = runBlocking {
        val id = seed("EXPENSE")
        repo.setHidden(id, hidden = true)
        assertEquals("EXCLUDED", dao.getById(id)!!.scope)
    }

    @Test
    fun `지출 복원은 예산 반영으로`() = runBlocking {
        val id = seed("EXPENSE", scope = "EXCLUDED")
        repo.setHidden(id, hidden = false)
        assertEquals("BUDGET", dao.getById(id)!!.scope)
    }

    @Test
    fun `수입 복원은 가계부만으로 — 복원이 추가 예산을 만들지 않는다`() = runBlocking {
        val id = seed("INCOME", scope = "EXCLUDED")
        repo.setHidden(id, hidden = false)
        assertEquals("LEDGER_ONLY", dao.getById(id)!!.scope)
    }

    @Test
    fun `setScope 는 지정한 상태를 그대로 쓴다`() = runBlocking {
        val id = seed("INCOME", scope = "LEDGER_ONLY")
        repo.setScope(id, TxScope.BUDGET)
        assertEquals("BUDGET", dao.getById(id)!!.scope)
    }
}

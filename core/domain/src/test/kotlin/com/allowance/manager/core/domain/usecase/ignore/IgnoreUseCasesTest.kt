package com.allowance.manager.core.domain.usecase.ignore

import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.usecase.FakeIgnoredAccountRepository
import com.allowance.manager.core.domain.usecase.InMemoryTransactionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 무시 = 되돌릴 수 없는 소급 삭제 — 어떤 내역이 지워지는지 정확해야 한다 */
class IgnoreUseCasesTest {

    private val transactions = InMemoryTransactionRepository()
    private val ignored = FakeIgnoredAccountRepository()

    private suspend fun tx(pkg: String, account: String?, amount: Long = 1_000L): Transaction {
        val id = transactions.record(
            Transaction(type = TransactionType.EXPENSE, amount = amount, packageName = pkg, sourceName = pkg,
                extractedAccount = account, createdAt = 1_000L),
        )
        return transactions.getById(id)!!
    }

    @Test
    fun `계좌번호 있는 내역을 무시하면 그 계좌만 — 같은 앱 다른 계좌는 남는다`() = runBlocking {
        val target = tx("com.bank", "941602-**-***318")
        tx("com.bank", "941602-**-***318", 2_000L)           // 같은 계좌 → 삭제
        tx("com.bank", "555555-**-***999")                   // 다른 계좌 → 유지
        tx("com.bank", null)                                 // 번호 없는 같은 앱 → 유지

        assertEquals(2, CountIgnorableTransactionsUseCase(transactions)(target))
        AddIgnoredAccountUseCase(ignored, transactions)(target)

        assertEquals(1, ignored.rows.size)
        assertEquals("941602*****318", ignored.rows[0].accountPattern)   // 구분자만 제거, 마스킹(*)은 보존
        assertEquals(listOf("555555-**-***999", null), transactions.rows.map { it.extractedAccount })
    }

    @Test
    fun `계좌번호 없는 내역을 무시하면 출처(앱) 기준 — 번호 있는 내역은 남는다`() = runBlocking {
        val target = tx("com.card", null)
        tx("com.card", null, 2_000L)                         // 같은 앱·번호 없음 → 삭제
        tx("com.card", "1234-**-5678")                       // 같은 앱·번호 있음 → 유지
        tx("com.other", null)                                // 다른 앱 → 유지

        assertEquals(2, CountIgnorableTransactionsUseCase(transactions)(target))
        AddIgnoredAccountUseCase(ignored, transactions)(target)

        assertTrue(ignored.rows[0].accountPattern.isEmpty())
        assertEquals("com.card", ignored.rows[0].packageName)
        assertEquals(2, transactions.rows.size)
        assertTrue(transactions.rows.none { it.packageName == "com.card" && it.extractedAccount == null })
    }
}

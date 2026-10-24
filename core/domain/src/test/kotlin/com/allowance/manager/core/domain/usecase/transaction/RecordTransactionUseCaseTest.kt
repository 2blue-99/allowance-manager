package com.allowance.manager.core.domain.usecase.transaction

import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.model.IgnoredAccount
import com.allowance.manager.core.domain.model.ParsedTransaction
import com.allowance.manager.core.domain.model.Transaction
import com.allowance.manager.core.domain.model.TransactionType
import com.allowance.manager.core.domain.model.TxScope
import com.allowance.manager.core.domain.usecase.FakeAccountRepository
import com.allowance.manager.core.domain.usecase.FakeDataStoreRepository
import com.allowance.manager.core.domain.usecase.FakeIgnoredAccountRepository
import com.allowance.manager.core.domain.usecase.InMemoryTransactionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 알림 → 거래 저장의 핵심 경로.
 * 무시 드롭 · 번호/출처 매칭 · 미등록 제외 · 수입 기본 가계부만 · 새 계좌 배지.
 */
class RecordTransactionUseCaseTest {

    private val transactions = InMemoryTransactionRepository()
    private val accounts = FakeAccountRepository()
    private val ignored = FakeIgnoredAccountRepository()
    private val dataStore = FakeDataStoreRepository()
    private val record = RecordTransactionUseCase(transactions, accounts, ignored, dataStore)

    private fun parsed(
        type: TransactionType = TransactionType.EXPENSE,
        amount: Long = 5_600L,
        packageName: String = "com.kbstar.kbbank",
        account: String? = "941602-**-***318",
    ) = ParsedTransaction(
        type = type, amount = amount, balance = null, packageName = packageName,
        sourceName = "KB국민은행", merchant = "스타벅스", extractedAccount = account, rawText = "",
    )

    @Test
    fun `무시 계좌(번호)에 매칭되면 저장하지 않고 null`() = runBlocking {
        ignored.add(IgnoredAccount(packageName = "com.kbstar.kbbank", sourceName = "KB", accountPattern = "94160212345318"))

        val id = record(parsed(account = "941602-**-***318"))

        assertNull(id)
        assertTrue(transactions.rows.isEmpty())
    }

    @Test
    fun `무시 출처(앱)에 매칭되면 계좌번호 없는 알림만 드롭`() = runBlocking {
        ignored.add(IgnoredAccount(packageName = "com.card", sourceName = "카드", accountPattern = ""))

        assertNull(record(parsed(packageName = "com.card", account = null)))
        // 같은 앱이라도 계좌번호가 있는 알림은 출처 무시 대상이 아님
        assertEquals(1L, record(parsed(packageName = "com.card", account = "1234-**-5678")))
    }

    @Test
    fun `등록 계좌(번호)에 매칭된 지출은 메인 + 예산 반영`() = runBlocking {
        val acc = accounts.seed(Account(packageName = "", bankName = "국민", accountPattern = "94160212345318"))

        val id = record(parsed())!!
        val saved = transactions.getById(id)!!

        assertEquals(acc.id, saved.accountId)
        assertEquals(TxScope.BUDGET, saved.scope)
        assertEquals("스타벅스", saved.merchant)
        assertFalse(saved.isManual)
    }

    @Test
    fun `등록 계좌 수입은 기본 '가계부만' — 자동으로 추가 예산이 되지 않는다`() = runBlocking {
        val acc = accounts.seed(Account(packageName = "", bankName = "국민", accountPattern = "94160212345318"))

        val id = record(parsed(type = TransactionType.INCOME, amount = 150_000L))!!
        val saved = transactions.getById(id)!!

        assertEquals(acc.id, saved.accountId)
        assertEquals(TxScope.LEDGER_ONLY, saved.scope)
        assertTrue(saved.inLedger)      // 홈 수입 카드·월별엔 보임
        assertFalse(saved.inBudget)     // 남은 금액엔 안 더해짐
    }

    @Test
    fun `출처 계좌(앱)는 계좌번호 없는 알림만 매칭한다`() = runBlocking {
        val acc = accounts.seed(Account(packageName = "com.card", bankName = "카드", accountPattern = ""))

        val noNumber = transactions.getById(record(parsed(packageName = "com.card", account = null))!!)!!
        val withNumber = transactions.getById(record(parsed(packageName = "com.card", account = "1234-**-5678"))!!)!!

        assertEquals(acc.id, noNumber.accountId)
        assertNull(withNumber.accountId)   // 번호 있는 알림은 번호 계좌로만 매칭 → 미등록
    }

    @Test
    fun `꺼진 계좌는 매칭하지 않는다`() = runBlocking {
        accounts.seed(Account(packageName = "", bankName = "국민", accountPattern = "94160212345318", enabled = false))

        val saved = transactions.getById(record(parsed())!!)!!

        assertNull(saved.accountId)
        assertEquals(TxScope.EXCLUDED, saved.scope)
    }

    @Test
    fun `미등록 알림은 제외 상태로 저장되고, 처음 보는 계좌면 새 계좌 배지를 켠다`() = runBlocking {
        val first = transactions.getById(record(parsed())!!)!!
        assertEquals(TxScope.EXCLUDED, first.scope)
        assertNull(first.accountId)
        assertTrue(dataStore.newAccountBadge)

        // 같은 계좌의 두 번째 알림은 배지를 다시 켜지 않는다 (이미 본 계좌)
        dataStore.newAccountBadge = false
        record(parsed(amount = 9_000L))
        assertFalse(dataStore.newAccountBadge)
    }

    @Test
    fun `환불(음수 지출)은 부호 그대로 저장된다`() = runBlocking {
        accounts.seed(Account(packageName = "", bankName = "국민", accountPattern = "94160212345318"))
        val saved = transactions.getById(record(parsed(amount = -5_600L))!!)!!
        assertEquals(-5_600L, saved.amount)
        assertEquals(TransactionType.EXPENSE, saved.type)
    }
}

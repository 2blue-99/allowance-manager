package com.allowance.manager.core.domain.usecase.account

import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.repository.AccountRepository
import com.allowance.manager.core.domain.usecase.FakeTransactionRepository
import com.allowance.manager.core.domain.usecase.transaction.DemoteFromMainUseCase
import com.allowance.manager.core.domain.usecase.transaction.PromoteToMainUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 메인 등록/해제는 서로의 역연산이다 — 내역 시트(ON/OFF→저장)와 계좌관리(스위치·삭제·추가) 모두
 * 계좌 단위로 기존 내역을 승격(→메인·예산)하거나 해제(→비메인·제외)해야 한다.
 */
class AccountRegistrationTest {

    private val accounts = FakeAccountRepository()
    private val transactions = RecordingTransactionRepository()

    @Test
    fun `계좌관리에서 추가하면 같은 번호의 기존 내역을 소급 승격한다`() = runBlocking {
        val id = AddAccountUseCase(accounts, transactions)(
            Account(packageName = "", bankName = "국민", accountPattern = "941602-12-345318")
        )
        assertEquals(listOf("number:94160212345318:$id"), transactions.calls)
    }

    @Test
    fun `같은 번호 계좌가 이미 있으면 추가해도 새 행을 만들지 않는다`() = runBlocking {
        // 온보딩 정보 단계 저장 후 프로세스가 죽어 다시 지나는 경우 · 계좌관리에서 같은 번호 재입력
        val first = AddAccountUseCase(accounts, transactions)(
            Account(packageName = "", bankName = "국민", accountPattern = "941602-12-345318", enabled = false)
        )
        val second = AddAccountUseCase(accounts, transactions)(
            Account(packageName = "", bankName = "내 계좌", accountPattern = "94160212345318")
        )

        assertEquals(first, second)
        assertEquals(1, accounts.all.value.size)
        // 재추가로 켜지고, 기본 이름("내 계좌")으로 기존 이름을 덮지 않는다
        assertTrue(accounts.byId(first)!!.enabled)
        assertEquals("국민", accounts.byId(first)!!.bankName)
        // 켜진 쪽(두 번째)만 소급 승격
        assertEquals(listOf("number:94160212345318:$first"), transactions.calls)
    }

    @Test
    fun `스위치 OFF는 그 계좌 내역을 해제하고 계좌는 목록에 남긴다`() = runBlocking {
        val acc = accounts.seed(Account(packageName = "", bankName = "국민", accountPattern = "94160212345318"))

        SetAccountEnabledUseCase(accounts, transactions)(acc, enabled = false)

        assertEquals(listOf("demote:${acc.id}"), transactions.calls)
        assertFalse(accounts.byId(acc.id)!!.enabled)
    }

    @Test
    fun `스위치 ON은 기존 내역을 다시 승격한다`() = runBlocking {
        val acc = accounts.seed(Account(packageName = "com.card", bankName = "카드", accountPattern = "", enabled = false))

        SetAccountEnabledUseCase(accounts, transactions)(acc, enabled = true)

        assertEquals(listOf("source:com.card:${acc.id}"), transactions.calls)
        assertTrue(accounts.byId(acc.id)!!.enabled)
    }

    @Test
    fun `삭제는 내역을 해제한 뒤 계좌를 지운다`() = runBlocking {
        val acc = accounts.seed(Account(packageName = "", bankName = "국민", accountPattern = "94160212345318"))

        DeleteAccountUseCase(accounts, transactions)(acc)

        assertEquals(listOf("demote:${acc.id}"), transactions.calls)
        assertEquals(null, accounts.byId(acc.id))
    }

    @Test
    fun `시트 OFF 저장은 계좌를 끄고 그 계좌 내역을 해제한다`() = runBlocking {
        val acc = accounts.seed(Account(packageName = "com.bank", bankName = "국민", accountPattern = "941602*****318"))

        DemoteFromMainUseCase(accounts, transactions)(acc.id)

        assertEquals(listOf("demote:${acc.id}"), transactions.calls)
        assertFalse(accounts.byId(acc.id)!!.enabled)
    }

    @Test
    fun `계좌가 이미 지워진 옛 내역도 시트 OFF로 해제된다`() = runBlocking {
        DemoteFromMainUseCase(accounts, transactions)(accountId = 99L)
        assertEquals(listOf("demote:99"), transactions.calls)
    }

    @Test
    fun `꺼진 같은 계좌가 있으면 시트 ON 저장은 새로 만들지 않고 다시 켠다`() = runBlocking {
        // 사용자가 직접 입력한 전체 번호 계좌 — 알림의 마스킹 번호와 같은 계좌
        val acc = accounts.seed(Account(packageName = "", bankName = "국민", accountPattern = "94160212345318", enabled = false))

        val id = PromoteToMainUseCase(accounts, transactions)("com.bank", "KB국민", "941602-**-***318")

        assertEquals(acc.id, id)
        assertEquals(1, accounts.all.value.size)
        assertTrue(accounts.byId(acc.id)!!.enabled)
        assertEquals(listOf("number:94160212345318:${acc.id}"), transactions.calls)
    }

    @Test
    fun `같은 계좌가 없으면 시트 ON 저장은 출처 계좌를 새로 등록한다`() = runBlocking {
        val id = PromoteToMainUseCase(accounts, transactions)("com.card", "현대카드", null)

        assertEquals("com.card", accounts.byId(id)!!.packageName)
        assertEquals(listOf("source:com.card:$id"), transactions.calls)
    }
}

/** 승격·해제 호출만 기록하는 저장소 */
private class RecordingTransactionRepository : FakeTransactionRepository() {
    val calls = mutableListOf<String>()
    override suspend fun promoteToMain(pattern: String, accountId: Long) { calls += "number:$pattern:$accountId" }
    override suspend fun promoteToMainBySource(packageName: String, accountId: Long) { calls += "source:$packageName:$accountId" }
    override suspend fun demoteFromMain(accountId: Long) { calls += "demote:$accountId" }
}

private class FakeAccountRepository : AccountRepository {
    val all = MutableStateFlow<List<Account>>(emptyList())
    private var nextId = 1L

    fun seed(account: Account): Account = account.copy(id = nextId++).also { all.value = all.value + it }
    fun byId(id: Long): Account? = all.value.firstOrNull { it.id == id }

    override fun observeAll(): Flow<List<Account>> = all
    override suspend fun getEnabled(): List<Account> = all.value.filter { it.enabled }
    override suspend fun getAll(): List<Account> = all.value
    override suspend fun getById(id: Long): Account? = byId(id)
    override suspend fun add(account: Account): Long = seed(account).id
    override suspend fun update(account: Account) {
        all.value = all.value.map { if (it.id == account.id) account else it }
    }
    override suspend fun delete(account: Account) {
        all.value = all.value.filterNot { it.id == account.id }
    }
}

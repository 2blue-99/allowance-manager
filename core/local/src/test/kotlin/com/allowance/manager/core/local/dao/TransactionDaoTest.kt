package com.allowance.manager.core.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.allowance.manager.core.local.database.AppDatabase
import com.allowance.manager.core.local.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 메인 승격/해제 SQL — 승격 시 제외(EXCLUDED) 내역은 지출·이체=BUDGET, **수입=LEDGER_ONLY** 로,
 * 사용자가 직접 정한 상태는 유지. 해제는 전부 비메인·EXCLUDED.
 * (실제 SQLite 로 CASE 문을 검증 — 인메모리 Room, Robolectric)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insert(
        type: String, scope: String, account: String? = "941602-**-***318", pkg: String = "com.bank", accountId: Long? = null,
    ): Long = dao.insert(
        TransactionEntity(type = type, amount = 1_000L, balance = null, packageName = pkg, sourceName = "s",
            extractedAccount = account, accountId = accountId, category = null, memo = null, scope = scope, createdAt = 1L),
    )

    @Test
    fun `promoteByIds — 제외된 지출은 예산 반영, 제외된 수입은 가계부만, 직접 정한 상태는 유지`() = runBlocking {
        val expense = insert("EXPENSE", "EXCLUDED")
        val income = insert("INCOME", "EXCLUDED")
        val transfer = insert("TRANSFER", "EXCLUDED")
        val userLedgerOnly = insert("EXPENSE", "LEDGER_ONLY")

        dao.promoteByIds(listOf(expense, income, transfer, userLedgerOnly), accountId = 7L)

        assertEquals("BUDGET", dao.getById(expense)!!.scope)
        assertEquals("LEDGER_ONLY", dao.getById(income)!!.scope)
        assertEquals("BUDGET", dao.getById(transfer)!!.scope)
        assertEquals("LEDGER_ONLY", dao.getById(userLedgerOnly)!!.scope)
        listOf(expense, income, transfer, userLedgerOnly).forEach { assertEquals(7L, dao.getById(it)!!.accountId) }
    }

    @Test
    fun `promoteBySource — 같은 앱의 계좌번호 없는 미매칭 내역만, 수입은 가계부만`() = runBlocking {
        val expense = insert("EXPENSE", "EXCLUDED", account = null, pkg = "com.card")
        val income = insert("INCOME", "EXCLUDED", account = null, pkg = "com.card")
        val withNumber = insert("EXPENSE", "EXCLUDED", account = "1234-**-5678", pkg = "com.card")
        val otherApp = insert("EXPENSE", "EXCLUDED", account = null, pkg = "com.other")
        val alreadyMain = insert("EXPENSE", "BUDGET", account = null, pkg = "com.card", accountId = 3L)

        dao.promoteBySource("com.card", accountId = 7L)

        assertEquals(7L, dao.getById(expense)!!.accountId); assertEquals("BUDGET", dao.getById(expense)!!.scope)
        assertEquals(7L, dao.getById(income)!!.accountId); assertEquals("LEDGER_ONLY", dao.getById(income)!!.scope)
        assertNull(dao.getById(withNumber)!!.accountId)          // 번호 있는 내역은 번호 매칭 대상
        assertNull(dao.getById(otherApp)!!.accountId)            // 다른 앱
        assertEquals(3L, dao.getById(alreadyMain)!!.accountId)   // 이미 메인인 건 건드리지 않음
    }

    @Test
    fun `demoteByAccount — 그 계좌 내역은 전부 비메인·EXCLUDED, 다른 계좌는 그대로`() = runBlocking {
        val mine1 = insert("EXPENSE", "BUDGET", accountId = 7L)
        val mine2 = insert("INCOME", "LEDGER_ONLY", accountId = 7L)
        val other = insert("EXPENSE", "BUDGET", accountId = 8L)

        dao.demoteByAccount(7L)

        listOf(mine1, mine2).forEach {
            assertNull(dao.getById(it)!!.accountId)
            assertEquals("EXCLUDED", dao.getById(it)!!.scope)
        }
        assertEquals(8L, dao.getById(other)!!.accountId)
        assertEquals("BUDGET", dao.getById(other)!!.scope)
    }

    @Test
    fun `updateContent 는 내용 컬럼만, updateScope 는 상태만 바꾼다`() = runBlocking {
        val id = insert("EXPENSE", "BUDGET", accountId = 7L)

        dao.updateContent(id, type = "EXPENSE", amount = -2_000L, merchant = "가게", memo = null, category = "CAFE")
        var row = dao.getById(id)!!
        assertEquals(-2_000L, row.amount); assertEquals("가게", row.merchant); assertEquals("CAFE", row.category)
        assertEquals(7L, row.accountId); assertEquals("BUDGET", row.scope)

        dao.updateScope(id, "LEDGER_ONLY")
        row = dao.getById(id)!!
        assertEquals("LEDGER_ONLY", row.scope); assertEquals(-2_000L, row.amount)
    }
}

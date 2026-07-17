package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.repository.AccountRepository
import com.allowance.manager.core.local.dao.AccountDao
import com.allowance.manager.core.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
) : AccountRepository {

    override fun observeAll(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getEnabled(): List<Account> =
        accountDao.getEnabled().map { it.toDomain() }

    override suspend fun add(account: Account): Long =
        accountDao.insert(account.toEntity())

    override suspend fun update(account: Account) =
        accountDao.update(account.toEntity())

    override suspend fun delete(account: Account) =
        accountDao.delete(account.toEntity())
}

private fun Account.toEntity() = AccountEntity(
    id = id,
    packageName = packageName,
    bankName = bankName,
    accountPattern = accountPattern,
    enabled = enabled,
)

private fun AccountEntity.toDomain() = Account(
    id = id,
    packageName = packageName,
    bankName = bankName,
    accountPattern = accountPattern,
    enabled = enabled,
)

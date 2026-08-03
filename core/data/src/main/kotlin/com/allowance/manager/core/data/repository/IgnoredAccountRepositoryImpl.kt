package com.allowance.manager.core.data.repository

import com.allowance.manager.core.domain.model.IgnoredAccount
import com.allowance.manager.core.domain.repository.IgnoredAccountRepository
import com.allowance.manager.core.local.dao.IgnoredAccountDao
import com.allowance.manager.core.local.entity.IgnoredAccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IgnoredAccountRepositoryImpl @Inject constructor(
    private val ignoredAccountDao: IgnoredAccountDao,
) : IgnoredAccountRepository {

    override fun observeAll(): Flow<List<IgnoredAccount>> =
        ignoredAccountDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<IgnoredAccount> =
        ignoredAccountDao.getAll().map { it.toDomain() }

    override suspend fun add(account: IgnoredAccount): Long =
        ignoredAccountDao.insert(account.toEntity())

    override suspend fun delete(id: Long) =
        ignoredAccountDao.deleteById(id)
}

private fun IgnoredAccount.toEntity() = IgnoredAccountEntity(
    id = id,
    packageName = packageName,
    sourceName = sourceName,
    accountPattern = accountPattern,
    createdAt = createdAt,
)

private fun IgnoredAccountEntity.toDomain() = IgnoredAccount(
    id = id,
    packageName = packageName,
    sourceName = sourceName,
    accountPattern = accountPattern,
    createdAt = createdAt,
)

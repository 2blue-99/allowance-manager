package com.allowance.manager.core.domain.usecase.account

import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    operator fun invoke(): Flow<List<Account>> = accountRepository.observeAll()
}

class AddAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(account: Account): Long = accountRepository.add(account)
}

class UpdateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(account: Account) = accountRepository.update(account)
}

class DeleteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(account: Account) = accountRepository.delete(account)
}

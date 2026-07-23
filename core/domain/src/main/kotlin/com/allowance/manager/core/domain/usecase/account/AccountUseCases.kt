package com.allowance.manager.core.domain.usecase.account

import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.model.MaskedAccount
import com.allowance.manager.core.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 계좌번호는 저장 직전 도메인 규칙으로 정규화한다(구분자 제거, 숫자·마스킹만 보존).
 * 호출부(온보딩·계좌관리·승격)가 각자 처리하면 빠뜨리기 쉬우므로 저장 길목에서 강제.
 */
private fun Account.normalized(): Account =
    copy(accountPattern = MaskedAccount.normalize(accountPattern))

class ObserveAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    operator fun invoke(): Flow<List<Account>> = accountRepository.observeAll()
}

class AddAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(account: Account): Long = accountRepository.add(account.normalized())
}

class UpdateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(account: Account) = accountRepository.update(account.normalized())
}

class DeleteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(account: Account) = accountRepository.delete(account)
}

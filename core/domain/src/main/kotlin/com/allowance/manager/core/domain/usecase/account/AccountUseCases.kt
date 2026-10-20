package com.allowance.manager.core.domain.usecase.account

import com.allowance.manager.core.domain.model.Account
import com.allowance.manager.core.domain.model.MaskedAccount
import com.allowance.manager.core.domain.repository.AccountRepository
import com.allowance.manager.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 계좌번호는 저장 직전 도메인 규칙으로 정규화한다(구분자 제거, 숫자·마스킹만 보존).
 * 호출부(온보딩·계좌관리·승격)가 각자 처리하면 빠뜨리기 쉬우므로 저장 길목에서 강제.
 */
private fun Account.normalized(): Account =
    copy(accountPattern = MaskedAccount.normalize(accountPattern))

/**
 * 메인 등록(ON)의 소급 적용 — 이 계좌에 해당하는 기존 비메인 내역을 메인으로 승격.
 * 번호 계좌는 계좌번호 자리별 대조, 출처 계좌는 계좌번호 없는 같은 앱 내역.
 * (해제(OFF)의 역연산은 [TransactionRepository.demoteFromMain])
 */
internal suspend fun TransactionRepository.promoteExisting(account: Account) {
    if (account.isNumberBased) {
        promoteToMain(account.accountPattern, account.id)
    } else if (account.packageName.isNotBlank()) {
        promoteToMainBySource(account.packageName, account.id)
    }
}

class ObserveAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    operator fun invoke(): Flow<List<Account>> = accountRepository.observeAll()
}

/** 계좌 추가(온보딩·계좌관리) — 저장 후 같은 계좌의 기존 내역도 바로 메인으로 승격 */
class AddAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(account: Account): Long {
        val normalized = account.normalized()
        val id = accountRepository.add(normalized)
        if (normalized.enabled) transactionRepository.promoteExisting(normalized.copy(id = id))
        return id
    }
}

/**
 * 계좌관리 스위치 ON/OFF = 메인 등록/해제.
 * - ON: 기존 내역 소급 승격 (꺼져 있던 사이 들어온 같은 계좌 내역 포함)
 * - OFF: 그 계좌 내역 전부 비메인·EXCLUDED → 메인 필터와 예산 집계에서 빠짐. 계좌는 목록에 남음
 */
class SetAccountEnabledUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(account: Account, enabled: Boolean) {
        val updated = account.normalized().copy(enabled = enabled)
        accountRepository.update(updated)
        if (enabled) {
            transactionRepository.promoteExisting(updated)
        } else {
            transactionRepository.demoteFromMain(updated.id)
        }
    }
}

/** 계좌 삭제 = 해제(내역 비메인·EXCLUDED) 후 목록에서 제거. 내역 자체는 전체 필터에 남는다 */
class DeleteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(account: Account) {
        transactionRepository.demoteFromMain(account.id)
        accountRepository.delete(account)
    }
}

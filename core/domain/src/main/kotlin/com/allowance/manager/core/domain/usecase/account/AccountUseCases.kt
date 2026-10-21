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

/**
 * 계좌 추가(온보딩·계좌관리) — 저장 후 같은 계좌의 기존 내역도 바로 메인으로 승격.
 *
 * 같은 계좌(번호 계좌 = 같은 번호 / 출처 계좌 = 같은 앱)가 이미 있으면 새로 만들지 않고 그 행을 재사용한다
 * (꺼져 있으면 켠다). 온보딩 중 프로세스가 죽어 정보 단계를 다시 지나거나, 계좌관리에서 같은 번호를
 * 또 넣어도 중복 행이 생기지 않는다. 계좌 테이블엔 unique 제약이 없어 여기서 막는다.
 */
class AddAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(account: Account): Long {
        val normalized = account.normalized()
        val existing = accountRepository.getAll().firstOrNull { it.isSameAccount(normalized) }
        val saved = if (existing != null) {
            // 이름은 새로 입력한 값으로 갱신(기본값 제외), 켜짐 상태로
            val merged = existing.copy(
                bankName = normalized.bankName.takeIf { it.isNotBlank() && it != DEFAULT_BANK_NAME } ?: existing.bankName,
                enabled = existing.enabled || normalized.enabled,
            )
            if (merged != existing) accountRepository.update(merged)
            merged
        } else {
            normalized.copy(id = accountRepository.add(normalized))
        }
        if (saved.enabled) transactionRepository.promoteExisting(saved)
        return saved.id
    }

    /** 같은 계좌 판정: 번호 계좌는 정규화 번호가 같을 때, 출처 계좌는 같은 앱일 때 */
    private fun Account.isSameAccount(other: Account): Boolean =
        if (isNumberBased || other.isNumberBased) {
            isNumberBased && other.isNumberBased && accountPattern == other.accountPattern
        } else {
            packageName.isNotBlank() && packageName == other.packageName
        }

    private companion object {
        /** 온보딩·계좌관리가 은행명을 비웠을 때 넣는 기본값 — 기존 이름을 이것으로 덮지 않는다 */
        const val DEFAULT_BANK_NAME = "내 계좌"
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

package com.allowance.manager.core.domain.usecase.ignore

import com.allowance.manager.core.domain.model.IgnoredAccount
import com.allowance.manager.core.domain.repository.IgnoredAccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 무시 계좌 목록 (설정 화면). */
class ObserveIgnoredAccountsUseCase @Inject constructor(
    private val ignoredAccountRepository: IgnoredAccountRepository,
) {
    operator fun invoke(): Flow<List<IgnoredAccount>> = ignoredAccountRepository.observeAll()
}

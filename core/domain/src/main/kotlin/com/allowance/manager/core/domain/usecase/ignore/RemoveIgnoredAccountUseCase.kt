package com.allowance.manager.core.domain.usecase.ignore

import com.allowance.manager.core.domain.repository.IgnoredAccountRepository
import javax.inject.Inject

/** 무시 해제 (이후 알림부터 다시 수신. 이미 삭제된 과거 내역은 복구되지 않음). */
class RemoveIgnoredAccountUseCase @Inject constructor(
    private val ignoredAccountRepository: IgnoredAccountRepository,
) {
    suspend operator fun invoke(id: Long) = ignoredAccountRepository.delete(id)
}

package com.allowance.manager.core.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

abstract class UseCase<in P, R>(private val coroutineDispatcher: CoroutineDispatcher) {

    suspend operator fun invoke(parameters: P): Result<R> = runCatching {
        withContext(coroutineDispatcher) {
            execute(parameters)
        }
    }

    protected abstract suspend fun execute(parameters: P): R
}

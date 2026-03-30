package com.allowance.manager.core.data.repository

import android.content.Context
import com.allowance.manager.core.domain.repository.AppVersionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppVersionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppVersionRepository {

    override fun getVersionName(): String =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName ?: ""
}

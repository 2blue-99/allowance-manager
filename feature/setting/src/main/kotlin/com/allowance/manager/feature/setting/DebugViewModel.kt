package com.allowance.manager.feature.setting

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.common.BaseViewModel
import com.allowance.manager.core.domain.usecase.setting.SetHomeGuideShownUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val setHomeGuideShownUseCase: SetHomeGuideShownUseCase,
) : BaseViewModel() {

    /** 홈 최초 진입 가이드 플래그 리셋 → 홈 재진입 시 다시 노출 */
    fun resetHomeGuide() {
        viewModelScope.launch { setHomeGuideShownUseCase(false) }
    }
}

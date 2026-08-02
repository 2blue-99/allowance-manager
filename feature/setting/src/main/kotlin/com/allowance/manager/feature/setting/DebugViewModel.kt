package com.allowance.manager.feature.setting

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.common.BaseViewModel
import com.allowance.manager.core.domain.usecase.setting.GetHomeGuideShownUseCase
import com.allowance.manager.core.domain.usecase.setting.SetHomeGuideShownUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    getHomeGuideShownUseCase: GetHomeGuideShownUseCase,
    private val setHomeGuideShownUseCase: SetHomeGuideShownUseCase,
) : BaseViewModel() {

    /** ON = 홈 가이드 표시(아직 안 본 상태). OFF = 표시 안 함(이미 본 것으로 처리). */
    val homeGuideEnabled: StateFlow<Boolean> =
        getHomeGuideShownUseCase()
            .map { shown -> !shown }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 토글 — ON이면 홈 재진입 시 가이드 노출, OFF면 노출 안 함 */
    fun setHomeGuideEnabled(enabled: Boolean) {
        viewModelScope.launch { setHomeGuideShownUseCase(!enabled) }
    }
}

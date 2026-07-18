package com.allowance.manager.feature.setting

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.usecase.setting.GetStatusBarEnabledUseCase
import com.allowance.manager.core.domain.usecase.setting.SetStatusBarEnabledUseCase
import com.allowance.manager.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    getStatusBarEnabledUseCase: GetStatusBarEnabledUseCase,
    private val setStatusBarEnabledUseCase: SetStatusBarEnabledUseCase,
) : BaseViewModel() {

    val statusBarEnabled: StateFlow<Boolean> = getStatusBarEnabledUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setStatusBarEnabled(enabled: Boolean) {
        viewModelScope.launch { setStatusBarEnabledUseCase(enabled) }
    }
}

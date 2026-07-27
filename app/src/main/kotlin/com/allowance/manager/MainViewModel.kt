package com.allowance.manager

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.usecase.config.CheckForceUpdateUseCase
import com.allowance.manager.core.domain.usecase.config.FetchRemoteConfigUseCase
import com.allowance.manager.core.domain.usecase.config.GetUpdateNoteUseCase
import com.allowance.manager.core.domain.usecase.onboarding.GetIntroShownUseCase
import com.allowance.manager.core.domain.usecase.onboarding.GetOnboardingDoneUseCase
import com.allowance.manager.core.domain.usecase.setting.GetStatusBarEnabledUseCase
import com.allowance.manager.core.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 앱 시작 시 첫 화면. (스플래시 판정을 MainActivity로 올려 SplashScreen API와 연동) */
enum class StartDestination { INTRO, ONBOARDING, HOME }

/** 시스템 스플래시 최소 표시 시간(ms). 판정이 너무 빨라 깜빡이는 것 방지. */
private const val MIN_SPLASH_MS = 900L

data class MainUiState(
    val showForceUpdateDialog: Boolean = false,
    val updateNote: String = "",
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val fetchRemoteConfigUseCase: FetchRemoteConfigUseCase,
    private val checkForceUpdateUseCase: CheckForceUpdateUseCase,
    private val getUpdateNoteUseCase: GetUpdateNoteUseCase,
    getStatusBarEnabledUseCase: GetStatusBarEnabledUseCase,
    getIntroShownUseCase: GetIntroShownUseCase,
    getOnboardingDoneUseCase: GetOnboardingDoneUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val statusBarEnabled: StateFlow<Boolean> = getStatusBarEnabledUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

     // null = 아직 판정 중 → 시스템 스플래시 유지. 최소 표시 시간이 지나야 실제 목적지 방출.
    val startDestination: StateFlow<StartDestination?> = combine(
        getIntroShownUseCase(),
        getOnboardingDoneUseCase(),
        flow { emit(false); delay(MIN_SPLASH_MS); emit(true) },
    ) { introShown, onboardingDone, minElapsed ->
        if (!minElapsed) {
            null
        } else when {
            !introShown -> StartDestination.INTRO
            !onboardingDone -> StartDestination.ONBOARDING
            else -> StartDestination.HOME
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        fetchRemoteConfig()
    }

    private fun fetchRemoteConfig() {
        viewModelScope.launch {
            runCatching { fetchRemoteConfigUseCase() }
                .onSuccess { checkForceUpdate() }
                .onFailure { setError(it.message) }
        }
    }

    private fun checkForceUpdate() {
        if (checkForceUpdateUseCase()) {
            _uiState.update {
                it.copy(
                    showForceUpdateDialog = true,
                    updateNote = getUpdateNoteUseCase(),
                )
            }
        }
    }
}

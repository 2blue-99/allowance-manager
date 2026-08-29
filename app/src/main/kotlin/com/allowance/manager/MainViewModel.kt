package com.allowance.manager

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.model.UpdateType
import com.allowance.manager.core.domain.usecase.alert.MarkAppSeenUseCase
import com.allowance.manager.core.domain.usecase.config.CheckAppUpdateUseCase
import com.allowance.manager.core.domain.usecase.config.FetchRemoteConfigUseCase
import com.allowance.manager.core.domain.usecase.config.MarkRecommendUpdateShownUseCase
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
    // null = 팝업 없음. FORCED = 강제(닫기 불가), RECOMMEND = 추천(확인/업데이트).
    val pendingUpdate: UpdateType? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val fetchRemoteConfigUseCase: FetchRemoteConfigUseCase,
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase,
    private val markRecommendUpdateShownUseCase: MarkRecommendUpdateShownUseCase,
    private val markAppSeenUseCase: MarkAppSeenUseCase,
    getStatusBarEnabledUseCase: GetStatusBarEnabledUseCase,
    getIntroShownUseCase: GetIntroShownUseCase,
    getOnboardingDoneUseCase: GetOnboardingDoneUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // 상태바 고정 알림은 "설정 켜짐 && 온보딩 완료"일 때만 시작한다.
    // (온보딩 중 알림 권한만 허용된 시점에 반쪽짜리 알림이 먼저 뜨는 것 방지)
    val statusBarEnabled: StateFlow<Boolean> = combine(
        getStatusBarEnabledUseCase(),
        getOnboardingDoneUseCase(),
    ) { enabled, onboardingDone -> enabled && onboardingDone }
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

    /** 앱을 조회한 시각 기록 — 가계부 관리 알림의 "안 본 내역" 판정 기준 갱신. (앱 진입/복귀 시 호출) */
    fun markAppSeen() {
        viewModelScope.launch { markAppSeenUseCase() }
    }

    private fun fetchRemoteConfig() {
        viewModelScope.launch {
            runCatching { fetchRemoteConfigUseCase() }
                .onSuccess { checkAppUpdate() }
                .onFailure { setError(it.message) }
        }
    }

    private suspend fun checkAppUpdate() {
        val type = checkAppUpdateUseCase() ?: return
        _uiState.update { it.copy(pendingUpdate = type) }
        // 추천은 하루 1회 — 띄운 순간 오늘 날짜를 기록해 같은 날 재노출 방지.
        if (type == UpdateType.RECOMMEND) markRecommendUpdateShownUseCase()
    }

    /** 추천 업데이트 팝업 '확인'/닫음 — 팝업만 내린다. (강제는 닫히지 않음) */
    fun onUpdateDialogDismissed() {
        _uiState.update { it.copy(pendingUpdate = null) }
    }
}

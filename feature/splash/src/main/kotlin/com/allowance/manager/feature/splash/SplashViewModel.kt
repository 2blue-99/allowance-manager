package com.allowance.manager.feature.splash

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.usecase.onboarding.GetIntroShownUseCase
import com.allowance.manager.core.domain.usecase.onboarding.GetOnboardingDoneUseCase
import com.allowance.manager.core.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class SplashDestination { INTRO, ONBOARDING, HOME }

@HiltViewModel
class SplashViewModel @Inject constructor(
    getIntroShownUseCase: GetIntroShownUseCase,
    getOnboardingDoneUseCase: GetOnboardingDoneUseCase,
) : BaseViewModel() {

    // null = 아직 로딩 중
    val destination: StateFlow<SplashDestination?> = combine(
        getIntroShownUseCase(),
        getOnboardingDoneUseCase(),
    ) { introShown, onboardingDone ->
        when {
            !introShown -> SplashDestination.INTRO
            !onboardingDone -> SplashDestination.ONBOARDING
            else -> SplashDestination.HOME
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

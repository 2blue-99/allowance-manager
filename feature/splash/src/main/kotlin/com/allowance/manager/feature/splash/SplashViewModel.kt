package com.allowance.manager.feature.splash

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.usecase.onboarding.GetOnboardingDoneUseCase
import com.allowance.manager.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    getOnboardingDoneUseCase: GetOnboardingDoneUseCase,
) : BaseViewModel() {

    // null = 아직 로딩 중
    val onboardingDone: StateFlow<Boolean?> = getOnboardingDoneUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

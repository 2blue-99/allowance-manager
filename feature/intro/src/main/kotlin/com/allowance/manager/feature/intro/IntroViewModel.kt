package com.allowance.manager.feature.intro

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.usecase.onboarding.SetIntroShownUseCase
import com.allowance.manager.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    private val setIntroShownUseCase: SetIntroShownUseCase,
) : BaseViewModel() {

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()

    fun finish() {
        viewModelScope.launch {
            setIntroShownUseCase()
            _isFinished.value = true
        }
    }
}

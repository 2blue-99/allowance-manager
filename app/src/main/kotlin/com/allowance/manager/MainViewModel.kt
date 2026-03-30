package com.allowance.manager

import androidx.lifecycle.viewModelScope
import com.allowance.manager.core.domain.usecase.config.CheckForceUpdateUseCase
import com.allowance.manager.core.domain.usecase.config.FetchRemoteConfigUseCase
import com.allowance.manager.core.domain.usecase.config.GetUpdateNoteUseCase
import com.allowance.manager.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val showForceUpdateDialog: Boolean = false,
    val updateNote: String = "",
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val fetchRemoteConfigUseCase: FetchRemoteConfigUseCase,
    private val checkForceUpdateUseCase: CheckForceUpdateUseCase,
    private val getUpdateNoteUseCase: GetUpdateNoteUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

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

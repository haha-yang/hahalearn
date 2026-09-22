package com.haha.mviFrame.main

import com.haha.baseui.mvi.IMviUiEffect
import com.haha.baseui.mvi.IMviUiState

data class MainUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList()
) : IMviUiState

sealed interface MainUiEffect : IMviUiEffect {
    data class ShowToast(val message: String) : MainUiEffect
}

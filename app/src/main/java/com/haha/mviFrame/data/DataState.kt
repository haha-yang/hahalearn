package com.haha.mviFrame.data

import com.haha.baseui.mvi.IMviUiEffect
import com.haha.baseui.mvi.IMviUiState
import com.haha.mviFrame.main.User

data class DataUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<User> = emptyList(),
    val error: String? = null,
    val selectedItem: User? = null,
    val searchQuery: String = ""
) : IMviUiState {
    val filteredItems: List<User>
        get() = if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { it.name.contains(searchQuery, true) }
        }
}

sealed interface DataUiEffect : IMviUiEffect {
    data class ShowToast(val message: String) : DataUiEffect
}

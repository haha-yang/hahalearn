package com.haha.mviFrame.main

import android.app.Application
import com.haha.baseui.mvi.BaseMVIViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) :
    BaseMVIViewModel<MainIntent, MainUiState, MainUiEffect>(
        application,
        MainUiState()
    ) {

    private val repository: MainRepository by lazy {
        MainRepository(ApiHelperImpl(RetrofitBuilder.apiService))
    }

    override suspend fun handleIntent(intent: MainIntent) {
        when (intent) {
            MainIntent.FetchUser -> fetchUser()
        }
    }

    private suspend fun fetchUser() {
        setState { copy(isLoading = true) }
        try {
            val users = withContext(Dispatchers.IO) { repository.getUsers() }
            setState { copy(isLoading = false, users = users) }
        } catch (e: Exception) {
            setState { copy(isLoading = false) }
            sendEffect(MainUiEffect.ShowToast(e.localizedMessage ?: "请求失败"))
        }
    }
}

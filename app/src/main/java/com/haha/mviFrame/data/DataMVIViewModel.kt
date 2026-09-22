package com.haha.mviFrame.data

import android.app.Application
import com.haha.baseui.mvi.BaseMVIViewModel
import kotlinx.coroutines.delay

class DataMVIViewModel(application: Application) :
    BaseMVIViewModel<DataIntent, DataUiState, DataUiEffect>(
        application,
        DataUiState()
    ) {

    override suspend fun handleIntent(intent: DataIntent) {
        when (intent) {
            is DataIntent.RequestData -> requestData()
            is DataIntent.RequestGuideInfo -> requestGuideInfo(intent.sn8)
        }
    }

    private suspend fun requestData() {
        setState { copy(isLoading = true, error = null) }
        try {
            delay(100)
            setState { copy(isLoading = false, items = emptyList()) }
        } catch (e: Exception) {
            setState { copy(isLoading = false, error = e.message) }
            sendEffect(DataUiEffect.ShowToast(e.message ?: "请求失败"))
        }
    }

    private suspend fun requestGuideInfo(sn8: String) {
        setState { copy(isLoading = true, error = null) }
        try {
            delay(100)
            setState { copy(isLoading = false) }
            sendEffect(DataUiEffect.ShowToast("已请求引导信息: $sn8"))
        } catch (e: Exception) {
            setState { copy(isLoading = false, error = e.message) }
            sendEffect(DataUiEffect.ShowToast(e.message ?: "请求失败"))
        }
    }
}

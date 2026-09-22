package com.haha.baseui.mvi

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.haha.baseui.mvvm.BaseViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 主流 MVI / 单向数据流 ViewModel：
 *
 * View --Intent--> ViewModel --UiState / UiEffect--> View
 *
 * - Intent 经 Channel 串行处理，避免并发 reduce 竞态
 * - UiState 用 StateFlow，可重放最新值（转屏、回到前台）
 * - UiEffect 用 Channel，只消费一次
 */
abstract class BaseMVIViewModel<I : IMviIntent, S : IMviUiState, E : IMviUiEffect>(
    application: Application,
    initialState: S
) : BaseViewModel(application) {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val intentChannel = Channel<I>(Channel.UNLIMITED)

    private val _uiEffect = Channel<E>(Channel.BUFFERED)
    val uiEffect: Flow<E> = _uiEffect.receiveAsFlow()

    protected val currentState: S
        get() = _uiState.value

    init {
        viewModelScope.launch {
            intentChannel.consumeAsFlow().collect { intent ->
                try {
                    handleIntent(intent)
                } catch (e: Exception) {
                    onIntentError(intent, e)
                }
            }
        }
    }

    /**
     * View 唯一入口。可在主线程直接调用，无需再包一层 coroutine。
     */
    fun sendIntent(intent: I) {
        if (!intentChannel.trySend(intent).isSuccess) {
            viewModelScope.launch { intentChannel.send(intent) }
        }
    }

    protected abstract suspend fun handleIntent(intent: I)

    /**
     * 以 reducer 方式更新状态，保证基于最新 state 做 copy。
     */
    protected fun setState(reducer: S.() -> S) {
        _uiState.update { it.reducer() }
    }

    protected fun sendEffect(effect: E) {
        if (!_uiEffect.trySend(effect).isSuccess) {
            viewModelScope.launch { _uiEffect.send(effect) }
        }
    }

    protected open fun onIntentError(intent: I, error: Throwable) {
        Log.e(TAG, "handleIntent failed: $intent", error)
    }

    override fun onCleared() {
        intentChannel.close()
        _uiEffect.close()
        super.onCleared()
    }
}

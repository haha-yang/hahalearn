package com.haha.baseui.mvi

import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.haha.baseui.mvvm.BaseMvvmFragment
import kotlinx.coroutines.launch

/**
 * MVI Fragment：在 View STARTED 时收集 [BaseMVIViewModel.uiState] / [BaseMVIViewModel.uiEffect]，
 * 停在后台或 View 销毁时自动停止，避免无效刷新和重复消费副作用。
 */
abstract class BaseMVIFragment<
        VB : ViewDataBinding,
        VM : BaseMVIViewModel<I, S, E>,
        I : IMviIntent,
        S : IMviUiState,
        E : IMviUiEffect
        > : BaseMvvmFragment<VB, VM>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeMvi()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun observeMvi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mViewModel.uiState.collect { render(it) }
                }
                launch {
                    mViewModel.uiEffect.collect { handleEffect(it) }
                }
            }
        }
    }

    protected fun sendIntent(intent: I) {
        mViewModel.sendIntent(intent)
    }

    /**
     * 根据最新 UiState 渲染界面。StateFlow 会先回放当前值。
     */
    protected abstract fun render(state: S)

    /**
     * 处理一次性副作用。无 Effect 的页面可保持空实现。
     */
    protected open fun handleEffect(effect: E) = Unit
}

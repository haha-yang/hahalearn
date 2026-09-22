package com.haha.baseui.mvi

/**
 * 用户意图。由 View 发出，ViewModel 串行消费。
 */
interface IMviIntent

/**
 * 不可变 UI 状态。每个页面用单一 data class 描述，由 [kotlinx.coroutines.flow.StateFlow] 持有。
 */
interface IMviUiState

/**
 * 一次性副作用：Toast、导航、Snackbar 等，不写入 [IMviUiState]，避免配置变更后重复消费。
 */
interface IMviUiEffect

/**
 * 页面无副作用时作为 [BaseMVIViewModel] 的 Effect 占位类型。
 */
object NoUiEffect : IMviUiEffect

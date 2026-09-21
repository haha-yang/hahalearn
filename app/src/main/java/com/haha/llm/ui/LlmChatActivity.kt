package com.haha.llm.ui

import android.view.View
import android.widget.Toast
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityLlmChatBinding
import com.haha.mviFrame.base.BaseMVIActivity
import com.haha.router.RoutePath
import com.haha.servicerouterannotation.annotation.Route

@Route(path = RoutePath.LLM_CHAT, name = "LlmChat")
class LlmChatActivity : BaseMVIActivity<
        ActivityLlmChatBinding,
        LlmChatViewModel,
        LlmChatIntent,
        LlmChatUiState,
        LlmChatUiEffect
        >() {

    override fun getLayoutId(): Int = R.layout.activity_llm_chat

    override fun getViewModelClass(): Class<LlmChatViewModel> = LlmChatViewModel::class.java

    override fun initView() {
        bindProviderListener()
        mViewDataBinding.cbStream.setOnClickListener { applyConfigFromViews() }
        mViewDataBinding.btnSend.setOnClickListener {
            sendIntent(LlmChatIntent.SendMessage(readInput(), readConfig()))
        }
        mViewDataBinding.btnStop.setOnClickListener {
            sendIntent(LlmChatIntent.Stop)
        }
        mViewDataBinding.btnClear.setOnClickListener {
            sendIntent(LlmChatIntent.Clear)
        }
    }

    override fun initData() = Unit

    override fun render(state: LlmChatUiState) {
        mViewDataBinding.tvEngine.text = "引擎：${state.engineName}"
        mViewDataBinding.tvUsage.text = state.usageLabel.ifBlank { "usage：—" }
        mViewDataBinding.groupHttp.visibility =
            if (state.httpConfigVisible) View.VISIBLE else View.GONE
        mViewDataBinding.tvTranscript.text = state.transcript.ifBlank { "（还没有 messages）" }
        mViewDataBinding.tvPartial.text = state.partialText.ifBlank { "（等待 delta.content）" }
        mViewDataBinding.tvRequest.text = buildString {
            if (state.requestUrl.isNotBlank()) {
                append(state.requestUrl)
                append('\n')
                append(state.requestHeader)
                append("\n\n")
            }
            append(state.requestJson.ifBlank { "发送后这里会看到 messages JSON" })
        }
        mViewDataBinding.tvSse.text = state.lastSse.ifBlank { "（尚无 SSE data 行）" }
        mViewDataBinding.btnSend.isEnabled = !state.isStreaming
        mViewDataBinding.btnStop.isEnabled = state.isStreaming
        mViewDataBinding.rbMock.isEnabled = !state.isStreaming
        mViewDataBinding.rbHttp.isEnabled = !state.isStreaming
        mViewDataBinding.cbStream.isEnabled = !state.isStreaming
    }

    override fun handleEffect(effect: LlmChatUiEffect) {
        when (effect) {
            is LlmChatUiEffect.ShowToast -> {
                Toast.makeText(this, effect.message, Toast.LENGTH_LONG).show()
            }

            is LlmChatUiEffect.FillConfig -> {
                mViewDataBinding.rgProvider.setOnCheckedChangeListener(null)
                mViewDataBinding.rbMock.isChecked = effect.config.mock
                mViewDataBinding.rbHttp.isChecked = !effect.config.mock
                bindProviderListener()
                mViewDataBinding.etBaseUrl.setText(effect.config.baseUrl)
                mViewDataBinding.etModel.setText(effect.config.model)
                mViewDataBinding.etApiKey.setText(effect.config.apiKey)
                mViewDataBinding.cbStream.isChecked = effect.config.stream
            }

            LlmChatUiEffect.ClearInput -> mViewDataBinding.etInput.text?.clear()
        }
    }

    private fun bindProviderListener() {
        mViewDataBinding.rgProvider.setOnCheckedChangeListener { _, _ ->
            applyConfigFromViews()
        }
    }

    private fun applyConfigFromViews() {
        sendIntent(LlmChatIntent.ApplyConfig(readConfig()))
    }

    private fun readInput(): String = mViewDataBinding.etInput.text?.toString().orEmpty()

    private fun readConfig(): LlmUiConfig {
        return LlmUiConfig(
            mock = mViewDataBinding.rbMock.isChecked,
            baseUrl = mViewDataBinding.etBaseUrl.text?.toString().orEmpty(),
            model = mViewDataBinding.etModel.text?.toString().orEmpty(),
            apiKey = mViewDataBinding.etApiKey.text?.toString().orEmpty(),
            stream = mViewDataBinding.cbStream.isChecked
        )
    }
}

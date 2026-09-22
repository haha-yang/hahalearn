package com.haha.speech.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.haha.baseui.mvi.BaseMVIActivity
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivitySpeechCaptionBinding
import com.haha.router.RoutePath
import com.haha.servicerouterannotation.annotation.Route

@Route(path = RoutePath.SPEECH_CAPTION, name = "SpeechCaption")
class SpeechCaptionActivity : BaseMVIActivity<
        ActivitySpeechCaptionBinding,
        SpeechCaptionViewModel,
        SpeechCaptionIntent,
        SpeechCaptionUiState,
        SpeechCaptionUiEffect
        >() {

    private val audioPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            sendIntent(SpeechCaptionIntent.RecognizeFile(uri.toString()))
        }

    override fun getLayoutId(): Int = R.layout.activity_speech_caption

    override fun getViewModelClass(): Class<SpeechCaptionViewModel> =
        SpeechCaptionViewModel::class.java

    override fun isUsedEncapsulatedPermissions(): Boolean = true

    override fun requestPermissionArray(): Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun initView() {
        mViewDataBinding.btnPrepareModel.setOnClickListener {
            sendIntent(SpeechCaptionIntent.PrepareModel)
        }
        mViewDataBinding.btnToggleCapture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "需要麦克风权限才能实时字幕", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendIntent(SpeechCaptionIntent.ToggleCapture)
        }
        mViewDataBinding.btnPickAudio.setOnClickListener {
            sendIntent(SpeechCaptionIntent.PickAudio)
        }
        mViewDataBinding.btnClear.setOnClickListener {
            sendIntent(SpeechCaptionIntent.ClearCaption)
        }
    }

    override fun initData() = Unit

    override fun render(state: SpeechCaptionUiState) {
        mViewDataBinding.tvEngine.text = "引擎：${state.engineName}"
        mViewDataBinding.tvPcm.text = "PCM：${state.pcmHint}"
        mViewDataBinding.tvModel.text = when (state.modelState) {
            SpeechModelState.Missing -> "模型：未准备（可先用 VAD 看流水线）"
            SpeechModelState.Downloading -> "模型：${state.downloadLabel}"
            SpeechModelState.Ready -> "模型：已就绪（离线中文）"
            SpeechModelState.Failed -> "模型：失败 ${state.error ?: ""}"
        }
        mViewDataBinding.progressDownload.visibility =
            if (state.modelState == SpeechModelState.Downloading) android.view.View.VISIBLE
            else android.view.View.GONE
        mViewDataBinding.progressDownload.progress = (state.downloadProgress * 100).toInt()
        mViewDataBinding.tvVad.text = if (state.isSpeech) "VAD：说话中" else "VAD：静音"
        mViewDataBinding.progressRms.progress = state.rmsPercent
        mViewDataBinding.tvCommitted.text = state.committedText
        mViewDataBinding.tvPartial.text = state.partialText
        mViewDataBinding.btnToggleCapture.text =
            if (state.isCapturing) "停止字幕" else "开始实时字幕"
        mViewDataBinding.btnPrepareModel.isEnabled =
            state.modelState != SpeechModelState.Downloading
        mViewDataBinding.btnPickAudio.isEnabled = !state.isCapturing
    }

    override fun handleEffect(effect: SpeechCaptionUiEffect) {
        when (effect) {
            is SpeechCaptionUiEffect.ShowToast -> {
                Toast.makeText(this, effect.message, Toast.LENGTH_LONG).show()
            }

            SpeechCaptionUiEffect.OpenAudioPicker -> {
                audioPicker.launch("audio/*")
            }
        }
    }

    override fun onStop() {
        sendIntent(SpeechCaptionIntent.StopCapture)
        super.onStop()
    }
}

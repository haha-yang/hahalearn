package com.haha.volume.ui

import android.content.Context
import android.content.IntentFilter
import android.graphics.Typeface
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityVolumeBinding
import com.haha.receiver.VolumeBroadReceiver
import com.haha.util.DpOrSpToPxTransfer
import com.haha.volume.lrc.LrcRow
import com.haha.volume.media.MediaStatus
import com.haha.volume.model.VolumeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 *     author : yangzy33
 *     time   : 2024-05-09
 *     desc   :
 *     version: 1.0
 */
class VolumeActivity : BaseMvvmActivity<ActivityVolumeBinding, VolumeViewModel>() {

    // 调节音量
    private lateinit var mAudioManager: AudioManager
    private lateinit var mVolumeBroadReceiver: VolumeBroadReceiver

    private lateinit var mVolumeAdjustAdapter: VolumeAdjustAdapter

    private var mLastLrcIndex = 0

    override fun getViewModelClass(): Class<VolumeViewModel> = VolumeViewModel::class.java

    override fun getLayoutId(): Int = R.layout.activity_volume

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initVolumeReceiver()
    }

    private fun initVolumeReceiver() {
        mVolumeBroadReceiver = VolumeBroadReceiver()
        mVolumeBroadReceiver.setCallback(object : VolumeBroadReceiver.VolumeCallback {
            override fun handleVolumeChange() {
                mVolumeAdjustAdapter.notifyDataSetChanged()
//                mViewModel?.setLeftChannel()
            }

            override fun handleNoisyAudioStream() {

            }
        })
        val intentFilter = IntentFilter()
        intentFilter.addAction(VolumeBroadReceiver.VOLUME_CHANGED_ACTION)
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(mVolumeBroadReceiver, intentFilter)
    }

    override fun initView() {
        mViewDataBinding?.apply {
            val layoutManager = LinearLayoutManager(mContext)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            rvVolume.layoutManager = layoutManager
            mVolumeAdjustAdapter = VolumeAdjustAdapter(mContext)
            val volumeList = mutableListOf(
                AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_ALARM,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_SYSTEM,
                AudioManager.STREAM_NOTIFICATION,
            )
            mVolumeAdjustAdapter.updateVolumeData(volumeList)
            mVolumeAdjustAdapter.setHasStableIds(true)
            rvVolume.adapter = mVolumeAdjustAdapter

            ivSongPrevious.setOnClickListener {
                playPreviousMusic()
            }
            ivSongPlay.setOnClickListener {
                Log.d(TAG, "mediastatus = ${mViewModel?.getMediaPlayerStatus()?.value?.name}")
                when (mViewModel?.getMediaPlayerStatus()?.value) {
                    MediaStatus.MEDIA_PLAYER_STATUS_START -> {
                        playPauseMusic()
                    }
                    MediaStatus.MEDIA_PLAYER_STATUS_PAUSE -> {
                        playResumeMusic()
                    }
                    else -> {
                        playMusic()
                    }
                }
            }
            ivSongNext.setOnClickListener {
                playNextMusic()
            }
            setPlayMode()

            sbMusicProgress.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val progress = seekBar?.progress ?: return
                    mViewModel?.apply {
                        val value = getMediaPlayerStatus().value
                        if (value == MediaStatus.MEDIA_PLAYER_STATUS_START ||
                            value == MediaStatus.MEDIA_PLAYER_STATUS_PAUSE) {
                            musicSeekTo(progress)
                            mViewDataBinding?.tvLeavingTime?.text =
                                calculateTime(progress.toLong())
                            Log.d(TAG, "onProgressChanged: progress = $progress")
                        } else {
                            Toast.makeText(mContext, "音频没有准备好, 暂不可调节", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }

            })
        }
    }

    private fun setPlayMode() {
        mViewModel?.apply {
            if (isSequentialPlayMode()) {
                mViewDataBinding?.ivPlayMode?.setImageResource(R.drawable.ic_play_mode_sequential)
            } else if (isShufflePlayMode()) {
                mViewDataBinding?.ivPlayMode?.setImageResource(R.drawable.ic_play_mode_shuffle)
            } else if (isLoopPlayMode()) {
                mViewDataBinding?.ivPlayMode?.setImageResource(R.drawable.ic_play_mode_loop)
            } else {
                playModeSequential()
                mViewDataBinding?.ivPlayMode?.setImageResource(R.drawable.ic_play_mode_sequential)
            }
        }
        mViewDataBinding?.ivPlayMode?.setOnClickListener {
            mViewModel?.apply {
                if (isSequentialPlayMode()) {
                    playModeShuffle()
                    mViewDataBinding?.ivPlayMode?.setImageResource(R.drawable.ic_play_mode_shuffle)
                } else if (isShufflePlayMode()) {
                    playModeLoop()
                    mViewDataBinding?.ivPlayMode?.setImageResource(R.drawable.ic_play_mode_loop)
                } else if (isLoopPlayMode()) {
                    playModeSequential()
                    mViewDataBinding?.ivPlayMode?.setImageResource(R.drawable.ic_play_mode_sequential)
                } else {
                    playModeSequential()
                    mViewDataBinding?.ivPlayMode?.setImageResource(R.drawable.ic_play_mode_sequential)
                }
            }
        }
    }

    override fun initData() {
        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        mViewModel?.apply {
            initMusic()
            initMusicList()

            getMediaPlayerStatus().observe(this@VolumeActivity) {
                Log.d(TAG, "media player status = $it")
                when (it) {
                    MediaStatus.MEDIA_PLAYER_STATUS_INIT -> {
                        mViewDataBinding?.apply {
                            ivSongPlay.background =
                                ResourcesCompat.getDrawable(resources, R.drawable.ic_pause, null)
                        }
                    }
                    MediaStatus.MEDIA_PLAYER_STATUS_PREPARING -> {

                    }
                    MediaStatus.MEDIA_PLAYER_STATUS_PREPARED -> {
                        playResumeMusic()
                    }
                    MediaStatus.MEDIA_PLAYER_STATUS_START -> {
                        mViewDataBinding?.apply {
                            ivSongPlay.background =
                                ResourcesCompat.getDrawable(resources, R.drawable.ic_play, null)
                        }
                    }
                    MediaStatus.MEDIA_PLAYER_STATUS_PAUSE -> {
                        mViewDataBinding?.apply {
                            ivSongPlay.background =
                                ResourcesCompat.getDrawable(resources, R.drawable.ic_pause, null)
                        }
                    }
                    MediaStatus.MEDIA_PLAYER_STATUS_STOP,
                    MediaStatus.MEDIA_PLAYER_STATUS_ERROR -> {
                        mViewDataBinding?.ivSongPlay?.background =
                            ResourcesCompat.getDrawable(resources, R.drawable.ic_pause, null)
                    }
                    MediaStatus.MEDIA_PLAYER_STATUS_COMPLETE -> {
                        Toast.makeText(mContext, "播放完成", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "playMode = ${mViewModel?.mPlayMode}")
                        if (isLoopPlayMode()) {
                            mViewDataBinding?.ivSongPlay?.background =
                                ResourcesCompat.getDrawable(resources, R.drawable.ic_play, null)
                        } else {
                            mViewDataBinding?.ivSongPlay?.background =
                                ResourcesCompat.getDrawable(resources, R.drawable.ic_pause, null)
                            playNextMusic()
                        }
                    }
                    MediaStatus.MEDIA_PLAYER_STATUS_END -> {

                    }
                    else -> {

                    }
                }
            }

            getMusicDuration().observe(this@VolumeActivity) {
                mViewDataBinding?.apply {
                    Log.d(TAG, "music duration = $it")
                    tvLeavingTime.text = calculateTime(0)
                    tvAmountTime.text = calculateTime(it.toLong())
                    sbMusicProgress.max = it
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        sbMusicProgress.min = 0
                    }
                }
            }

            getMusicProgress().observe(this@VolumeActivity) {
                var currentSongLrc = ""
                mViewModel?.apply {
                    val lrcRows = getLrcRows().value
                    if (!lrcRows.isNullOrEmpty()) {
                        currentSongLrc = lrcRows[getCurrentLrcIndex()].content ?: ""
                    }
                }
                mViewDataBinding?.apply {
                    tvLeavingTime.text = calculateTime(it.toLong())
                    sbMusicProgress.progress = it
                    val content = tvSongLrc.text.toString()
                    val start = content.indexOf(currentSongLrc)
                    if (start != -1) {
                        val end = start + currentSongLrc.length
                        val spanStr= SpannableString(content)
                        val boldSpan = StyleSpan(Typeface.BOLD)
                        spanStr.setSpan(boldSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        val fgdColorSpan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            ForegroundColorSpan(mContext.resources.getColor(R.color.blue, null))
                        } else {
                            ForegroundColorSpan(mContext.resources.getColor(R.color.blue))
                        }
                        spanStr.setSpan(fgdColorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        val sizeSpan = AbsoluteSizeSpan(DpOrSpToPxTransfer.sp2px(mContext, 18))
                        spanStr.setSpan(sizeSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        tvSongLrc.text = spanStr

                        if (mLastLrcIndex >= 2 && mLastLrcIndex != getCurrentLrcIndex()) {
                            nsvSongLrc.smoothScrollTo(0, tvSongLrc.lineHeight * (getCurrentLrcIndex() - 2))
                        }
                        mLastLrcIndex = getCurrentLrcIndex()
                    }
                }
            }

            getMusicCacheProgress().observe(this@VolumeActivity) {
                mViewDataBinding?.apply {
                    sbMusicProgress.secondaryProgress = it
                }
            }

            getLrcRows().observe(this@VolumeActivity) {
                viewModelScope.launch(Dispatchers.Main) {
                    resetObtainSongInfo(it)
                }
            }
        }
    }

    private fun playMusic() {
        mViewModel?.playCurrentMusic()
    }

    private fun playResumeMusic() {
        mViewModel?.apply {
            musicStart()
            startTimerTask()
        }
    }

    private fun playPauseMusic() {
        mViewModel?.apply {
            musicPause()
            stopTimerTask()
        }
    }

    private fun playNextMusic() {
        mViewModel?.playNextMusic()
    }

    private fun playPreviousMusic() {
        mViewModel?.playPreviousMusic()
    }

    private fun resetObtainSongInfo(lrcRows: MutableList<LrcRow>?) {
        mViewDataBinding?.apply {
            val songName = mViewModel?.getSongInfo()?.let {
                "《${it.songName}》-${it.singer}"
            } ?: "播放音乐"
            tvSongName.text = songName
            val sb = StringBuilder()
            if (!lrcRows.isNullOrEmpty()) {
                lrcRows.forEach { row ->
                    row.content?.let {
                        sb.append("$it\n")
                    }
                }
            }
            if (sb.isEmpty()) {
                nsvSongLrc.visibility = View.GONE
            } else {
                nsvSongLrc.visibility = View.VISIBLE
                tvSongLrc.text = sb.toString().trim()
                val vto = tvSongLrc.viewTreeObserver
                vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout()  {
                        val lineCount = tvSongLrc.lineCount
                        val lineHeight = tvSongLrc.lineHeight
                        val layoutParams = nsvSongLrc.layoutParams
                        if (lineCount * lineHeight > layoutParams.height) {
                            layoutParams.height = 5 * lineHeight
                        }
                        nsvSongLrc.layoutParams = layoutParams
                        nsvSongLrc.requestLayout()
                        tvSongLrc.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mVolumeBroadReceiver)
        mViewModel?.onDestroy()
    }
}
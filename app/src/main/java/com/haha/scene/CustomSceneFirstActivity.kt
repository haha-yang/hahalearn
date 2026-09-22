package com.haha.scene

import android.os.Build
import android.transition.Scene
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityCustomSceneFirstBinding

class CustomSceneFirstActivity : BaseMvvmActivity<ActivityCustomSceneFirstBinding, BaseViewModel>() {
    private val flContent: FrameLayout by lazy {
        findViewById(R.id.flContent)
    }

    private val tvText: TextView by lazy {
        findViewById(R.id.tvText)
    }

    private val ivFirstImage: TextView by lazy {
        findViewById(R.id.ivFirstImage)
    }

    private var isFirst = true
    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun getLayoutId(): Int = R.layout.activity_custom_scene_first

    override fun initView() {
        val firstScene = Scene.getSceneForLayout(flContent, R.layout.layout_first_scene, this)
        val secondScene = Scene.getSceneForLayout(flContent, R.layout.layout_second_scene, this)

        tvText.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (isFirst) {
                    TransitionManager.go(secondScene, Slide(Gravity.TOP).removeTarget(R.id.ivFirstImage))
                } else {
                    TransitionManager.go(firstScene, Slide(Gravity.BOTTOM))
                }
                isFirst = !isFirst
            }
        }
    }

    override fun initData() {
    }
}
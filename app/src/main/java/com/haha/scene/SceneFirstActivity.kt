package com.haha.scene

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.transition.Slide
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.ImageView
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivitySceneFirstBinding

/**
 * <pre>
 *     author : yangzy33
 *     e-mail : yangzy33@midea.com
 *     time   : 2022/10/27
 *     desc   :
 *     version: 1.0
 * </pre>
 */
class SceneFirstActivity : BaseMvvmActivity<ActivitySceneFirstBinding, BaseViewModel>() {
    private var activity: Activity? = null

    private val ivImage: ImageView by lazy {
        findViewById(R.id.ivFirstImage)
    }

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun getLayoutId(): Int = R.layout.activity_scene_first

    override fun requestFeature() {
        super.requestFeature()
        setSceneAnimation()
    }

    override fun initView() {
        activity = this

        ivImage.transitionName = "activityTransform"
        ivImage.setOnClickListener { v: View? ->
            //判断Android版本
//                val bundle =
//                    ActivityOptions.makeSceneTransitionAnimation(activity, ivImage,"activityTransform")
//                        .toBundle()
            val bundle = ActivityOptions.makeSceneTransitionAnimation(activity).toBundle()
            startActivity(Intent(activity, SceneSecondActivity::class.java), bundle)
        }
    }

    override fun initData() {
    }

    private fun setSceneAnimation() {
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        window.allowEnterTransitionOverlap = false
        Slide().apply {
            slideEdge = Gravity.BOTTOM
            duration = 300
            excludeTarget(android.R.id.statusBarBackground, true)
            excludeTarget(android.R.id.navigationBarBackground, true)
        }.also {
            window.exitTransition = it
            window.enterTransition = it
            window.reenterTransition = it
        }

    }
}
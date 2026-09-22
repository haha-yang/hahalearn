package com.haha.baseui

import android.R
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.haha.log.DOFLogUtil


/**
 *     author : yangzy33
 *     time   : 2024-08-12
 *     desc   :
 *     version: 1.0
 */
abstract class BaseFragment : Fragment() {
    protected open val TAG = javaClass.simpleName
    protected lateinit var mRootView: View
    protected lateinit var mContext: Context

    protected var mUseThemeStatusBarColor = false

    protected var mUseStatusBarColor = true

    protected abstract fun getLayoutId(): Int

    protected open fun getNavigationBarColor(): Int = R.color.transparent

    /**
     * 顶部状态栏不展示时，背景颜色需要设置为透明色
     */
    protected open fun getStatusBarColor(): Int = R.color.transparent

    protected open fun isShowNavigation(): Boolean = true

    protected open fun isBlackStatusText(): Boolean = true

    protected open fun isShowStatus(): Boolean = true

    protected open fun getRootViewId(): Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mContext = requireContext()
        mRootView = inflater.inflate(getLayoutId(), container, false)
        return mRootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStatusBarColor(getStatusBarColor())
        setNavigationBarColor(getNavigationBarColor())
        handleNavigationVAndStatusVisibility()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart()")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause()")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop()")
    }

    /**
     * 设置导航栏背景颜色
     *
     * @param 背景颜色
     */
    private fun setNavigationBarColor(color: Int) {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        requireActivity().window.navigationBarColor = resources.getColor(color)
    }

    /**
     * 设置状态栏背景颜色
     *
     * @param color 背景颜色
     */
    private fun setStatusBarColor(color: Int) {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        requireActivity().window.statusBarColor = resources.getColor(color)
    }

    private fun handleNavigationVAndStatusVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            handleAdvancedSystemNavigationAndStatus()
        } else {
            handleNormalSystemNavigationAndStatus()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun handleAdvancedSystemNavigationAndStatus() {
        if (requireActivity().window.insetsController == null) {
            handleNormalSystemNavigationAndStatus()
            return
        }
        DOFLogUtil.d(TAG, "handleAdvancedSystemNavigationAndStatus")
        val isShowNavigation = isShowNavigation()
        val isShowStatus = isShowStatus()
        val isBlackStatusText = isBlackStatusText()
        requireActivity().window.insetsController!!.apply {
            // TODO 隐藏导航栏，上滑仍会会显示，后续再研究
            if (isShowNavigation) {
                show(WindowInsetsCompat.Type.navigationBars())
            } else {
                hide(WindowInsetsCompat.Type.navigationBars())
            }
            if (isShowStatus) {
                show(WindowInsetsCompat.Type.statusBars())
            } else {
                // TODO 顶部有黑边，暂时不生效，后续再研究
//                hide(WindowInsetsCompat.Type.statusBars())
//                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                val uiOption =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN.or(View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
                requireActivity().window.decorView.systemUiVisibility = uiOption
                Color.TRANSPARENT
                requireActivity().window.statusBarColor = resources.getColor(getStatusBarColor())
                addStatusBarView()
            }
            if (isBlackStatusText) {
                setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            }
        }
    }

    private fun handleNormalSystemNavigationAndStatus() {
        DOFLogUtil.d(TAG, "handleNormalSystemNavigationAndStatus")
        val isShowNavigation = isShowNavigation()
        val isShowStatus = isShowStatus()
        val isBlackStatusText = isBlackStatusText()
        var uiOption = View.SYSTEM_UI_FLAG_VISIBLE
        DOFLogUtil.d(TAG, "uiOption = $uiOption")
        // 设置隐藏导航栏
        if (!isShowNavigation) {
            uiOption = uiOption.or(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                .or(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) // 增加沉淀式体验, 即底部上划可展示导航栏，会自动消失
            DOFLogUtil.d(TAG, "uiOption = $uiOption, isShowNavigation = $isShowNavigation")
        }
        if (isBlackStatusText) {
            uiOption = uiOption.or(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
                .or(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            DOFLogUtil.d(TAG, "uiOption = $uiOption, isBlackStatusText = $isBlackStatusText")
        }
        // 设置状态栏字体颜色为黑色
        if (!isShowStatus) {
            uiOption = uiOption.or(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                .or(View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            Color.TRANSPARENT
            requireActivity().window.statusBarColor = resources.getColor(getStatusBarColor())
            addStatusBarView()
            DOFLogUtil.d(TAG, "uiOption = $uiOption, isShowStatus = $isShowStatus")
        }
        DOFLogUtil.d(TAG, "uiOption = $uiOption")
        requireActivity().window.decorView.systemUiVisibility = uiOption
    }

    /**
     * 把创建的StatusBar添加到布局中
     */
    protected fun addStatusBarView() {
        if (getRootViewId() != 0) {
            val rootView = requireActivity().findViewById<ViewGroup>(getRootViewId())
            DOFLogUtil.d(TAG, "rootView = $rootView")
            if (rootView != null) {
                rootView.fitsSystemWindows = true
                // 在原来的位置上添加一个状态栏
                val statusBarView = createStatusBarView(requireActivity())
                statusBarView.fitsSystemWindows = true
                DOFLogUtil.d(
                    TAG,
                    "statusBarView = $statusBarView, height = ${statusBarView.height}"
                )
                rootView.addView(statusBarView, 0)
                rootView.requestLayout()
            }
        }
    }

    /**
     * 创建一个需要填充statusBarView
     */
    private fun createStatusBarView(activity: Activity): View {
        val statusBarView = View(activity)
        statusBarView.background = ResourcesCompat.getDrawable(resources, getStatusBarColor(), null)
        val statusBarParams: ViewGroup.LayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(activity)
        )
        statusBarView.layoutParams = statusBarParams
        return statusBarView
    }

    /**
     * 获取状态栏的高度
     */
    private fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        DOFLogUtil.d(TAG, "getStatusBarHeight = $result")
        return result
    }
}
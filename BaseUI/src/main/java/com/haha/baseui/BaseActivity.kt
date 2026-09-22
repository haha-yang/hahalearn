package com.haha.baseui

import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsCompat
import com.haha.log.DOFLogUtil
import com.haha.main.timeMonitor.TimeMonitorConfig
import com.haha.main.timeMonitor.TimeMonitorManager


/**
 *     author : yangzy33
 *     time   : 2024-05-11
 *     desc   :
 *     version: 1.0
 */
abstract class BaseActivity : AppCompatActivity() {
    protected open val TAG = javaClass.simpleName

    protected lateinit var mContext: Context

    protected var mUseThemeStatusBarColor = false

    protected var mUseStatusBarColor = true

    protected abstract fun getLayoutId(): Int

    /**
     * 隐藏标题栏[ActionBar]
     *
     * 适配版本号[Build.VERSION_CODES.UPSIDE_DOWN_CAKE]时，需要主动设置主题背景
     */
    protected open fun hideTitleAndActionBar() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
            supportActionBar?.hide()
        }
    }

    protected open fun getNavigationBarColor(): Int = android.R.color.transparent

    /**
     * 顶部状态栏不展示时，背景颜色需要设置为透明色
     */
    protected open fun getStatusBarColor(): Int = android.R.color.transparent

    protected open fun isShowNavigation(): Boolean = true

    protected open fun isBlackStatusText(): Boolean = true

    protected open fun isShowStatus(): Boolean = true

    protected open fun getRootViewId(): Int = 0

    /**
     * 是否在 [onCreate] 里同步 inflate。
     * 首页等需要 [androidx.asynclayoutinflater.view.AsyncLayoutInflater] 的页面返回 false，
     * 自行在主线程 [setContentView] 后再走后续内容初始化。
     */
    protected open fun shouldInflateContentInOnCreate(): Boolean = true

    protected open fun inflateContentView() {
        setContentView(getLayoutId())
    }

    /**
     * 上层额外状态设置
     *
     * 适配版本号[Build.VERSION_CODES.UPSIDE_DOWN_CAKE]时，需要主动设置主题背景
     */
    protected open fun requestFeature() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // 主题仍在 lib-common：Theme.HahaLearn.NoActionBar。
            // BaseUI 不反向依赖 lib-common，这里从宿主合并后的资源表解析。
            val themeId = resources.getIdentifier(
                "Theme.HahaLearn.NoActionBar",
                "style",
                packageName
            )
            if (themeId != 0) {
                setTheme(themeId)
            }
        }
    }

    protected open fun extraConfig() {

    }

    /**
     * [hideTitleAndActionBar] 之后、同步 inflate 之前。
     * 首页开屏容器须在这里 [setContentView]，避免 extraConfig 过早加 content 导致 requestFeature 崩溃。
     */
    protected open fun onWindowReady() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("BaseActivity_requestFeature_before")
        requestFeature()
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("BaseActivity_onCreate_before")
        super.onCreate(savedInstanceState)
        mContext = this
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("BaseActivity_extraConfig_before")
        extraConfig()
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("BaseActivity_hideTitleAndActionBar_before")
        hideTitleAndActionBar()
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("BaseActivity_onWindowReady_before")
        onWindowReady()
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("BaseActivity_inflateContentView_before")
        if (shouldInflateContentInOnCreate()) {
            inflateContentView()
        }
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("BaseActivity_setStatusBarColor_before")
        setStatusBarColor(getStatusBarColor())
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("BaseActivity_setNavigationBarColor_before")
        setNavigationBarColor(getNavigationBarColor())
        // insetsController / decorView 依赖 DecorView；异步 inflate 时须等 setContentView 后再处理
        if (shouldInflateContentInOnCreate()) {
            TimeMonitorManager.getInstance()
                .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
                .recodingTimeTag("BaseActivity_handleNavigationVAndStatusVisibility_before")
            handleNavigationVAndStatusVisibility()
        }
    }

    /**
     * 设置导航栏背景颜色
     *
     * @param 背景颜色
     */
    private fun setNavigationBarColor(color: Int) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.navigationBarColor = resources.getColor(color)
    }

    /**
     * 设置状态栏背景颜色
     *
     * @param color 背景颜色
     */
    private fun setStatusBarColor(color: Int) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(color)
    }

    /**
     * 须在 [setContentView] / DecorView 就绪后调用。
     * 异步 inflate 场景请在 bind 完成后显式调用。
     */
    protected fun handleNavigationVAndStatusVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            handleAdvancedSystemNavigationAndStatus()
        } else {
            handleNormalSystemNavigationAndStatus()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun handleAdvancedSystemNavigationAndStatus() {
        // peekDecorView：Decor 未安装时为 null；直接读 insetsController 会在部分机型 NPE
        if (window.peekDecorView() == null) {
            handleNormalSystemNavigationAndStatus()
            return
        }
        val insetsController = window.insetsController
        if (insetsController == null) {
            handleNormalSystemNavigationAndStatus()
            return
        }
        DOFLogUtil.d(TAG, "handleAdvancedSystemNavigationAndStatus")
        val isShowNavigation = isShowNavigation()
        val isShowStatus = isShowStatus()
        val isBlackStatusText = isBlackStatusText()
        insetsController.apply {
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
                window.decorView.systemUiVisibility = uiOption
                Color.TRANSPARENT
                window.statusBarColor = resources.getColor(getStatusBarColor())
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
            window.statusBarColor = resources.getColor(getStatusBarColor())
            addStatusBarView()
            DOFLogUtil.d(TAG, "uiOption = $uiOption, isShowStatus = $isShowStatus")
        }
        DOFLogUtil.d(TAG, "uiOption = $uiOption")
        window.decorView.systemUiVisibility = uiOption
    }

    /**
     * 把创建的StatusBar添加到布局中
     */
    protected fun addStatusBarView() {
        if (getRootViewId() != 0) {
            val rootView = findViewById<ViewGroup>(getRootViewId())
            DOFLogUtil.d(TAG, "rootView = $rootView")
            if (rootView != null) {
                rootView.fitsSystemWindows = true
                // 在原来的位置上添加一个状态栏
                val statusBarView = createStatusBarView(this)
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
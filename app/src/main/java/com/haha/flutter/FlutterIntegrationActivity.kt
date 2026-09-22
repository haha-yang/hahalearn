package com.haha.flutter

import android.content.Context
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.lifecycleScope
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityFlutterIntegrationBinding
import io.flutter.embedding.android.FlutterFragment
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.launch

/**
 * @auther: haha
 * @Date:   2026/1/11
 * @Detail:
 */
class FlutterIntegrationActivity :
    BaseMvvmActivity<ActivityFlutterIntegrationBinding, BaseViewModel>() {

    private var mFlutterChannel: FlutterChannel? = null
    private var mMethodChannel: MethodChannel? = null

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun initView() {
        // 在Activity中使用
        val statusBarHeight = getStatusBarHeight(this)
        Log.d(TAG, "Height: $statusBarHeight")

        val statusView = View(this).apply {
            id = View.generateViewId()
            setBackgroundColor(resources.getColor(getStatusBarColor()))
        }
        addViewToTopAndPushDown(
            mViewDataBinding.container,
            statusView,
            statusBarHeight,
            ConstraintSet.MATCH_CONSTRAINT,
            R.id.fragment_flutter_container
        )
    }

    // 顶部添加状态栏
    fun addViewToTopAndPushDown(
        constraintLayout: ConstraintLayout,
        newView: View,
        newViewHeight: Int,
        newViewWidth: Int,
        viewToPushDownId: Int? = null
    ) {
        newView.id = View.generateViewId()
        constraintLayout.addView(newView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        // 设置新View的约束
        constraintSet.connect(
            newView.id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP
        )
        constraintSet.connect(
            newView.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START
        )
        constraintSet.connect(
            newView.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END
        )
        constraintSet.constrainHeight(newView.id, newViewHeight) // 设置具体高度
        constraintSet.constrainWidth(newView.id, newViewWidth)

        // 如果指定了要下移的View，则修改其约束
        if (viewToPushDownId != null) {
            constraintSet.connect(
                viewToPushDownId,
                ConstraintSet.TOP,
                newView.id,
                ConstraintSet.BOTTOM
            )
        } else {
            // 将所有原本连接到父布局顶部的View下移
            pushAllViewsDown(constraintLayout, constraintSet, newView.id)
        }

        constraintSet.applyTo(constraintLayout)
    }

    private fun pushAllViewsDown(
        constraintLayout: ConstraintLayout,
        constraintSet: ConstraintSet,
        newViewId: Int
    ) {
        // 遍历所有子View
        for (i in 0 until constraintLayout.childCount) {
            val child = constraintLayout.getChildAt(i)
            if (child.id == newViewId) continue // 跳过新添加的View

            val params = child.layoutParams as? ConstraintLayout.LayoutParams ?: continue

            // 如果View原本顶部连接到父布局，则改为连接到新View的底部
            if (params.topToTop == ConstraintSet.PARENT_ID) {
                constraintSet.connect(
                    child.id,
                    ConstraintSet.TOP,
                    newViewId,
                    ConstraintSet.BOTTOM
                )
            }
        }
    }

    override fun initData() {
        initFlutter()
        // 调用 Flutter 方法
        callFlutterFunction()
    }

    // Kotlin
    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier(
            "status_bar_height", "dimen", "android"
        )
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun initFlutter() {
        // 统一 get-or-create，避免重复建 Engine / 重复执行 Dart 入口
        val flutterEngine = FlutterChannel.getOrCreateEngine(applicationContext)

        // 创建 MethodChannel
        mMethodChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            FlutterChannel.CHANNEL_NAME
        )

        // 添加 Flutter Fragment
        val flutterFragment =
            FlutterFragment.withCachedEngine(FlutterChannel.FLUTTER_ENGINE_ID)
                .build<FlutterFragment>()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_flutter_container, flutterFragment)
            .commit()
    }

    private fun callFlutterFunction() {
        lifecycleScope.launch {
            try {
                mMethodChannel?.invokeMethod(
                    "getFlutterData",
                    "来自 Kotlin 的请求",
                    object : MethodChannel.Result {
                        override fun success(result: Any?) {
                            // MethodChannel 回调已在主线程
                            println("收到 Flutter 响应: $result")
                        }

                        override fun error(
                            errorCode: String,
                            errorMessage: String?,
                            errorDetails: Any?
                        ) {
                            Log.e(
                                TAG,
                                "error: errorCode = $errorCode, errorMessage = $errorMessage, errorDetails = $errorDetails"
                            )
                        }

                        override fun notImplemented() {
                            Log.i(TAG, "notImplemented")
                        }
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        testFlutterCalls()
    }

    private fun testFlutterCalls() {
        // 初始化 Flutter Channel（applicationContext，避免持有 Activity）
        mFlutterChannel = FlutterChannel(applicationContext).also {
            it.initialize()
            it.testFlutterCalls()
        }
    }

    override fun isShowStatus(): Boolean = false

    override fun isShowNavigation(): Boolean = false

    override fun getLayoutId(): Int = R.layout.activity_flutter_integration

    override fun onDestroy() {
        mMethodChannel?.setMethodCallHandler(null)
        mMethodChannel = null
        // 先解绑 Channel / 取消协程，再统一销毁共享 Engine（只 destroy 一次）
        mFlutterChannel?.destroy()
        mFlutterChannel = null
        FlutterChannel.releaseCachedEngine()
        super.onDestroy()
    }
}
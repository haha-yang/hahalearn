package com.haha.baseui.mvvm

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.haha.baseui.BaseActivity
import com.haha.main.timeMonitor.TimeMonitorConfig
import com.haha.main.timeMonitor.TimeMonitorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 *     author : yangzy33
 *     time   : 2024-05-13
 *     desc   :
 *     version: 1.0
 */
abstract class BaseMvvmActivity<V : ViewDataBinding, VM : BaseViewModel> : BaseActivity() {

    private var _binding: V? = null
    protected val mViewDataBinding: V
        get() = _binding
            ?: error("binding 未初始化，须先走 inflateContentView")

    protected fun isBindingInitialized(): Boolean = _binding != null

    protected val mViewModel: VM by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val modelClass: Class<VM> = getViewModelClass()
        val viewModel = ViewModelProvider(this)[modelClass]
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            lifecycle.addObserver(viewModel)
        }
        ViewModelProvider(this).get(modelClass)
        viewModel
    }

    private var mRequestPermissionLauncher: ActivityResultLauncher<Array<String>>? = null

    override fun inflateContentView() {
        _binding = DataBindingUtil.setContentView(this, getLayoutId())
        _binding?.lifecycleOwner = this
    }

    /**
     * 内容 View 就绪后的 MVVM 初始化（背景、状态栏占位、权限、initView/initData）。
     */
    protected open fun onContentReady() {
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("BaseMvvmActivity_create")

        mViewDataBinding.root.background =
            ContextCompat.getDrawable(mContext, defaultBackgroundId())
        if (!isShowStatus()) {
            addStatusBarView()
        }
        mRequestPermissionLauncher?.launch(requestPermissionArray())

        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("BaseMvvmActivity_initView_before")
        initView()
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("BaseMvvmActivity_initData_before")
        initData()
    }

    protected abstract fun getViewModelClass(): Class<VM>

    protected abstract fun initView()

    protected abstract fun initData()

    protected open fun isUsedEncapsulatedPermissions(): Boolean = false

    protected open fun requestPermissionArray(): Array<String> = emptyArray()

    protected open fun handlePermissionResult(permissionResultMap: Map<String, Boolean>) {

    }

    protected open fun defaultBackgroundId(): Int = android.R.color.white

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        if (isUsedEncapsulatedPermissions()) {
            mRequestPermissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionResultMap ->
                    handlePermissionResult(permissionResultMap)
                }
        }
        onContentReady()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        _binding?.unbind()
        _binding = null
        super.onDestroy()
    }
}

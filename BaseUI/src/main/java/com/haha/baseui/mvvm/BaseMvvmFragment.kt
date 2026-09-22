package com.haha.baseui.mvvm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.haha.baseui.BaseFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 *     author : yangzy33
 *     time   : 2024-08-12
 *     desc   :
 *     version: 1.0
 */
abstract class BaseMvvmFragment<V : ViewDataBinding, VM : BaseViewModel> : BaseFragment() {

    protected lateinit var mViewDataBinding: V

    protected val mViewModel: VM by lazy {
        val modelClass: Class<VM> = getViewModelClass()
        val viewModel = ViewModelProvider(this)[modelClass]
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            lifecycle.addObserver(viewModel)
        }
        viewModel
    }

    protected var mRequestPermissionLauncher: ActivityResultLauncher<Array<String>>? = null

    protected abstract fun getViewModelClass(): Class<VM>

    abstract fun initView()

    abstract fun initData()

    protected open fun isUsedEncapsulatedPermissions(): Boolean = false

    protected open fun requestPermissionArray(): Array<String> = emptyArray()

    protected open fun handlePermissionResult(permissionResultMap: Map<String, Boolean>) {

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        mViewDataBinding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false)
        mViewDataBinding.root.background =
            ContextCompat.getDrawable(mContext, android.R.color.white)
        mRootView = mViewDataBinding.root
        return mViewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isShowStatus()) {
            addStatusBarView()
        }
        if (isUsedEncapsulatedPermissions()) {
            mRequestPermissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionResultMap ->
                    handlePermissionResult(permissionResultMap)
                }
        }
        mRequestPermissionLauncher?.launch(requestPermissionArray())
        initView()
        initData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mViewDataBinding.unbind()
    }
}
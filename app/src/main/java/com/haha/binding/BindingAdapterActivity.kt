package com.haha.binding

import androidx.recyclerview.widget.LinearLayoutManager
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityBindingAdapterBinding

/**
 * ViewBinding + BindingAdapter 示例页。
 * 布局通过 Data Binding 绑定 ViewModel，自定义属性由 [BindingAdapters] 处理。
 */
class BindingAdapterActivity :
    BaseMvvmActivity<ActivityBindingAdapterBinding, BindingAdapterViewModel>() {

    private val tagAdapter = BindingAdapterTagAdapter()

    override fun getViewModelClass(): Class<BindingAdapterViewModel> =
        BindingAdapterViewModel::class.java

    override fun getLayoutId(): Int = R.layout.activity_binding_adapter

    override fun initView() {
        mViewDataBinding.vm = mViewModel
        mViewDataBinding.lifecycleOwner = this

        mViewDataBinding.rvTags.apply {
            layoutManager = LinearLayoutManager(this@BindingAdapterActivity)
            adapter = tagAdapter
        }

        mViewDataBinding.btnToggleDetail.setOnClickListener {
            mViewModel.toggleDetailVisible()
        }
    }

    override fun initData() {
        mViewModel.loadDemoData()
        mViewModel.tags.observe(this) { tagAdapter.submitList(it) }
    }
}

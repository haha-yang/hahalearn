package com.haha.selector

import android.util.Log
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivitySelectorBinding


/**
 *     author : yangzy33
 *     time   : 2024-05-09
 *     desc   :
 *     version: 1.0
 */
class SelectorActivity: BaseMvvmActivity<ActivitySelectorBinding, BaseViewModel>() {
    private var mCount = 0

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun getLayoutId(): Int = R.layout.activity_selector

    override fun initView() {
        mViewDataBinding.apply {
            tvDs.setOnClickListener {
                mCount ++
                tvClickCount.text = mCount.toString()
                when (mCount % 6) {
                    0 -> {
                        tvDsEnable.isEnabled = true

                        tvDsPress.isEnabled = true
                        tvDsPress.isClickable = true
                        tvDsPress.isFocusable = true
                        tvDsPress.requestFocus()
                    }
                    1 -> {
                        tvDsEnable.isEnabled = false

                        tvDsPress.isEnabled = true
                        tvDsPress.isClickable = false
                        tvDsPress.isFocusable = true
                        tvDsPress.requestFocus()
                    }
                    2 -> {
                        tvDsPress.isEnabled = true
                        tvDsPress.isClickable = true
                        tvDsPress.isFocusable = false
                        tvDsPress.requestFocus()
                    }
                    3 -> {
                        tvDsPress.isEnabled = false
                        tvDsPress.isClickable = true
                        tvDsPress.isFocusable = false
                        tvDsPress.requestFocus()
                    }
                    4 -> {
                        tvDsPress.isEnabled = true
                        tvDsPress.isClickable = false
                        tvDsPress.isFocusable = false
                        tvDsPress.requestFocus()
                    }
                    5 -> {
                        tvDsPress.isEnabled = false
                        tvDsPress.isClickable = false
                        tvDsPress.isFocusable = true
                        tvDsPress.requestFocus()
                    }
                }
            }

            cbSelect.setDefaultAllDrawables()
            cbSelect.setOnClickListener {
                cbSelect.isChecked = !cbSelect.isChecked
                if (cbSelect.isChecked) {
                    cbSelect.background = getDrawable(R.drawable.drawable_shape_checkbox_select)
                } else {
                    cbSelect.background = getDrawable(R.drawable.drawable_shape_checkbox_null)
                }
                Log.d(TAG, "setOnClickListener: isChecked = ${cbSelect.isChecked}")
            }


        }
    }

    override fun initData() {
    }
}
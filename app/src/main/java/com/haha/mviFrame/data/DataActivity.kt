package com.haha.mviFrame.data

import android.view.View
import android.widget.Toast
import com.haha.baseui.mvi.BaseMVIActivity
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityDataBinding

class DataActivity : BaseMVIActivity<
        ActivityDataBinding,
        DataMVIViewModel,
        DataIntent,
        DataUiState,
        DataUiEffect
        >() {

    override fun getLayoutId(): Int = R.layout.activity_data

    override fun getViewModelClass(): Class<DataMVIViewModel> = DataMVIViewModel::class.java

    override fun initView() {
        mViewDataBinding.buttonFetchUser.setOnClickListener {
            sendIntent(DataIntent.RequestData("1"))
        }
        mViewDataBinding.progressBar.setOnClickListener {
            sendIntent(DataIntent.RequestGuideInfo("12345678"))
        }
    }

    override fun initData() = Unit

    override fun render(state: DataUiState) {
        mViewDataBinding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        mViewDataBinding.buttonFetchUser.visibility =
            if (state.isLoading) View.GONE else View.VISIBLE
    }

    override fun handleEffect(effect: DataUiEffect) {
        when (effect) {
            is DataUiEffect.ShowToast -> {
                Toast.makeText(this, effect.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}

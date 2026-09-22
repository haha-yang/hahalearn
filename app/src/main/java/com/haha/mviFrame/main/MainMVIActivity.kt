package com.haha.mviFrame.main

import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.haha.baseui.mvi.BaseMVIActivity
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityMviBinding

class MainMVIActivity : BaseMVIActivity<
        ActivityMviBinding,
        MainViewModel,
        MainIntent,
        MainUiState,
        MainUiEffect
        >() {

    private val adapter = MainAdapter(arrayListOf())

    override fun getLayoutId(): Int = R.layout.activity_mvi

    override fun getViewModelClass(): Class<MainViewModel> = MainViewModel::class.java

    override fun initView() {
        mViewDataBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        mViewDataBinding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )
        mViewDataBinding.recyclerView.adapter = adapter
        mViewDataBinding.buttonFetchUser.setOnClickListener {
            sendIntent(MainIntent.FetchUser)
        }
    }

    override fun initData() = Unit

    override fun render(state: MainUiState) {
        mViewDataBinding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        val showList = state.users.isNotEmpty()
        mViewDataBinding.recyclerView.visibility = if (showList) View.VISIBLE else View.GONE
        mViewDataBinding.buttonFetchUser.visibility =
            if (state.isLoading || showList) View.GONE else View.VISIBLE
        if (showList) {
            adapter.submitUsers(state.users)
        }
    }

    override fun handleEffect(effect: MainUiEffect) {
        when (effect) {
            is MainUiEffect.ShowToast -> {
                Toast.makeText(this, effect.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}

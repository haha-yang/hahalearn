package com.haha.easyswipemenulayout

import android.content.Context
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityEasySwipeMenuBinding
import com.haha.log.DOFLogUtil


/**
 *     author : yangzy33
 *     time   : 2024-05-23
 *     desc   :
 *     version: 1.0
 */
class EasySwipeMenuActivity : BaseMvvmActivity<ActivityEasySwipeMenuBinding, BaseViewModel>() {
    private lateinit var easySwipeMenuAdapter: EasySwipeMenuAdapter
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private val listData: MutableList<String> by lazy {
        mutableListOf()
    }

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun getLayoutId(): Int = R.layout.activity_easy_swipe_menu

    override fun initView() {
        val wm: WindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        DOFLogUtil.d(TAG, "match_parent = ${wm.defaultDisplay.width}")
        mLinearLayoutManager = LinearLayoutManager(this)
        easySwipeMenuAdapter = EasySwipeMenuAdapter(this)
        mViewDataBinding?.recyclerview?.apply {
            this.layoutManager = mLinearLayoutManager
            adapter = easySwipeMenuAdapter
            viewTreeObserver.addOnGlobalLayoutListener(listener)
        }
    }

    private val listener: OnGlobalLayoutListener by lazy {
        OnGlobalLayoutListener {
            val width = mViewDataBinding?.recyclerview?.width ?: 0
            val itemWidth = mLinearLayoutManager.width
            DOFLogUtil.d(TAG, "viewTreeObserver: width = $width, itemWidth = $itemWidth")
            mViewDataBinding?.recyclerview?.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
        }
    }

    override fun initData() {
        for (i in 0..19) {
            listData.add("index is =$i")
        }
        easySwipeMenuAdapter.updateData(listData)
        easySwipeMenuAdapter.notifyDataSetChanged()
    }

    override fun getNavigationBarColor(): Int {
        return R.color.yellow
    }

    override fun getStatusBarColor(): Int {
        return R.color.yellow
    }
}


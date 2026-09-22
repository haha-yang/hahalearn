package com.haha.bluetooth.adapter

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.haha.baseui.BaseFragment


/**
 *     author : yangzy33
 *     time   : 2024-08-13
 *     desc   :
 *     version: 1.0
 */
class ViewPagerFragmentStateAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity) {
    private val mDataList = mutableListOf<BaseFragment>()

    override fun getItemCount(): Int = mDataList.size

    override fun createFragment(position: Int): BaseFragment {
        return mDataList[position]
    }

    fun addFragment(fragment: BaseFragment) {
        mDataList.add(fragment)
    }
}
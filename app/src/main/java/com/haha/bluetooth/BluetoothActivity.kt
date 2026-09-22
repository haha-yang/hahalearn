package com.haha.bluetooth

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.bluetooth.adapter.ViewPagerFragmentStateAdapter
import com.haha.bluetooth.fragment.BleBondedFragment
import com.haha.bluetooth.fragment.BleClientFragment
import com.haha.bluetooth.fragment.BleScanFragment
import com.haha.bluetooth.fragment.BleServerFragment
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityBluetoothBinding

/**
 *     author : yangzy33
 *     time   : 2024-07-04
 *     version: 1.0
 */
class BluetoothActivity : BaseMvvmActivity<ActivityBluetoothBinding, BaseViewModel>() {

    companion object {
        const val REQUEST_CODE_PERMISSION = 101
        const val REQUEST_CODE_BLUETOOTH_DISCOVERABLE = 102
    }

    private lateinit var mBleViewPager: ViewPager2
    private lateinit var mTabLayout: TabLayout
    private val mTabTitleList = mutableListOf<String>()
    private var mOnPageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun getLayoutId(): Int = R.layout.activity_bluetooth

    override fun initView() {
        mBleViewPager = mViewDataBinding.bleViewPager
        mTabLayout = mViewDataBinding.bleTabLayout
        val pagerAdapter = ViewPagerFragmentStateAdapter(this@BluetoothActivity).apply {
            addFragment(BleClientFragment())
            addFragment(BleScanFragment())
            addFragment(BleBondedFragment())
            addFragment(BleServerFragment())
        }
        mBleViewPager.apply {
            adapter = pagerAdapter
            isUserInputEnabled = false
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            currentItem = 0
            mOnPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    mTabLayout.getTabAt(position)?.select()
                }
            }
            registerOnPageChangeCallback(mOnPageChangeCallback!!)
        }
        mTabTitleList.add("蓝牙客户端")
        mTabTitleList.add("蓝牙扫描")
        mTabTitleList.add("蓝牙绑定")
        mTabTitleList.add("蓝牙服务端")
        mTabLayout.apply {
            mTabTitleList.forEach {
                val tab = mTabLayout.newTab()
                tab.text = it
                addTab(tab)
            }
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.apply {
                        val selectTab = text.toString().trim()
                        val spannableStr = SpannableString(selectTab)
                        val styleSpan = StyleSpan(Typeface.BOLD)
                        spannableStr.setSpan(
                            styleSpan,
                            0,
                            selectTab.length,
                            Spanned.SPAN_INCLUSIVE_INCLUSIVE
                        )
                        tab.text = spannableStr
                        mBleViewPager.currentItem = position
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    tab?.apply {
                        val unSelectTab = text.toString().trim()
                        val spannableStr = SpannableString(unSelectTab)
                        val styleSpan = StyleSpan(Typeface.NORMAL)
                        spannableStr.setSpan(
                            styleSpan,
                            0,
                            unSelectTab.length,
                            Spanned.SPAN_INCLUSIVE_INCLUSIVE
                        )
                        tab.text = spannableStr
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                }
            })
        }
        TabLayoutMediator(mTabLayout, mBleViewPager) { tab, position ->
            tab.text = mTabTitleList[position]
        }.attach()
    }

    override fun initData() {
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_BLUETOOTH_DISCOVERABLE) {
            Log.d(TAG, "resultCode = ${resultCode}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSION -> {
                for (i in permissions.indices) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Log.d(
                            TAG,
                            "onRequestPermissionsResult: permission = ${permissions[i]}, grantValue = ${grantResults[i]}"
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mOnPageChangeCallback?.let { mBleViewPager.unregisterOnPageChangeCallback(it) }
        mBleViewPager.adapter?.let {
            it.notifyItemRangeRemoved(0, it.itemCount - 1)
        }
    }
}
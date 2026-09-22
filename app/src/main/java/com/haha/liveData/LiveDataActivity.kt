package com.haha.liveData

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityLiveDataBinding

/**
 *
 * @author:     haha
 * @date:       2025/3/26
 * Description:
 *
 **/
class LiveDataActivity : BaseMvvmActivity<ActivityLiveDataBinding, BaseViewModel>() {
    companion object {
        var mLiveData: MutableLiveData<String>? = null
    }

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun initView() {
        mViewDataBinding.btnMainSend.setOnClickListener {
            mLiveData?.value = "main send"
        }
        mViewDataBinding.btnThreadSend.setOnClickListener {
            Thread {
                mLiveData?.postValue("thread send")
            }.start()
        }
        mViewDataBinding.btnEnterActivity.setOnClickListener {

        }

        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d(TAG, "onStateChanged: $event")
            }
        })

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                Log.d(TAG, "onCreate: ${owner.lifecycle}")
            }

            override fun onDestroy(owner: LifecycleOwner) {
                Log.d(TAG, "onDestroy: ${owner.lifecycle}")
            }

            override fun onStart(owner: LifecycleOwner) {
                Log.d(TAG, "onStart: ${owner.lifecycle}")
            }

            override fun onStop(owner: LifecycleOwner) {
                Log.d(TAG, "onStop: ${owner.lifecycle}")
            }

            override fun onResume(owner: LifecycleOwner) {
                Log.d(TAG, "onResume: ${owner.lifecycle}")
            }

            override fun onPause(owner: LifecycleOwner) {
                Log.d(TAG, "onPause: ${owner.lifecycle}")
            }
        })
    }

    override fun initData() {
        mLiveData = MutableLiveData()
        mLiveData?.observe(this) {
            val value = mLiveData?.value
            Log.d(TAG, "value = $value")
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_live_data
}
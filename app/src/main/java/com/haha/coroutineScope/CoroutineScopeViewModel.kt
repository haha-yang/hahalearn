package com.haha.coroutineScope

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.haha.baseui.mvvm.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 *
 * @author:     haha
 * @date:       2025/2/11
 * Description: 协程数据Model
 *
 **/
class CoroutineScopeViewModel(application: Application): BaseViewModel(application) {

    private var mSSID = "abcd"
    @Volatile
    private var mIsPreciseMatch: Boolean = false
    private var mScanDeviceJob : Job? = null
    private var mFindDeviceJob : Job? = null

    private var scopeTest: CoroutineScopeTest? = null

    private val _mSsidFirstData: MutableLiveData<String> = MutableLiveData()
    val mSsidFirstData: LiveData<String> = _mSsidFirstData

    private val _mSsidSecondData: MutableLiveData<String> = MutableLiveData()
    val mSsidSecondData: LiveData<String> = _mSsidSecondData

    private val _mToastNotifyLiveData: SingleLiveData<String> = SingleLiveData()
    val mToastNotifyLiveData: SingleLiveData<String> = _mToastNotifyLiveData

    private val _mediatorLiveData: MediatorLiveData<String> = MediatorLiveData()
    val mediatorLiveData: LiveData<String> = _mediatorLiveData

    init {
        _mediatorLiveData.addSource<String>(
            _mSsidFirstData,
            Observer<String> {
                _mediatorLiveData.value = it
            })
        _mediatorLiveData.addSource<String>(
            _mSsidSecondData,
            Observer<String> {
                _mediatorLiveData.value = it
            })
    }

    fun startScan() {
        val array = initScanData()
        for (s in array) {
            filterDevice(s)
        }

        scopeTest = CoroutineScopeTest()
        scopeTest?.start()
         val mediatorLiveData = MediatorLiveData<String>()
        mediatorLiveData.addSource(mSsidFirstData) {
            Log.d(TAG, "onChange1: ${it}")
        }
        _mSsidSecondData.postValue("haha")
        mToastNotifyLiveData.value = "hello"
    }

    private fun initScanData(): MutableList<String> {
        val array = ArrayList<String>()
        array.add("abce")
        array.add("abeef")
        array.add("abcefd")
        array.add("abcefd")
        array.add("abafdce")
        array.add("abcfdae")
        array.add("abcefad")
        array.add("abdefdce")
        array.add("addbce")
        array.add("abce")
        array.add("adfdbce")
        array.add("addbce")
        array.add("addbce")
        array.add("adbce")
        array.add("abcDEd")
        array.add("abcfdfdde")
        array.add("abcDed")
        array.add("abcDED")
        array.add("abcDEd")
        array.add("abcefd")
        array.add("abcefd")
        array.add("abafdce")
        array.add("abcfdae")
        array.add("abcefad")
        array.add("abdefdce")
        return array
    }

    private fun filterDevice(s: String) {
        mScanDeviceJob = viewModelScope.launch(Dispatchers.Default) {
            if (mIsPreciseMatch) {
                Log.d(
                    TAG,
                    "mIsPreciseMatch = $mIsPreciseMatch, mSSID = $mSSID, s = $s, ${Thread.currentThread()} hahahahh"
                )
                if (mIsPreciseMatch && mSSID == s) {
                    launch(Dispatchers.Main) {
                        if (mIsPreciseMatch) {
                            _mSsidFirstData.value = s
                            Log.d(
                                TAG,
                                "mIsPreciseMatch = $mIsPreciseMatch, mSSID = $mSSID, s = $s, ${Thread.currentThread()} hahahahh"
                            )
                        }
                    }
                }
            } else {
                Log.d(
                    TAG,
                    "mIsPreciseMatch = $mIsPreciseMatch, mSSID = $mSSID, s = $s, ${Thread.currentThread()} mFindDeviceJob"
                )
                if (!mIsPreciseMatch && s.contains(mSSID, true)) {
                    launch(Dispatchers.Main) {
                        if (!mIsPreciseMatch) {
                            mIsPreciseMatch = true
                            mSSID = s
                            _mSsidSecondData.value = s
                            Log.d(
                                TAG,
                                "mIsPreciseMatch = $mIsPreciseMatch, mSSID = $mSSID, s = $s, Dispatchers.Main mFindDeviceJob"
                            )
                        }
                    }
                }
            }
        }
    }

    fun stopScan() {
        mScanDeviceJob?.cancel()
        scopeTest?.stop()
    }

    override fun onCleared() {
        stopScan()
        super.onCleared()
    }
}

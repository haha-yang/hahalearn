package com.haha.baseui.mvvm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleObserver
import io.reactivex.disposables.CompositeDisposable


/**
 *     author : yangzy33
 *     time   : 2024-05-13
 *     desc   :
 *     version: 1.0
 */
open class BaseViewModel(application: Application) : AndroidViewModel(application),
    LifecycleObserver {
    protected val TAG = javaClass.simpleName

    protected val mApplication = application

    var mCompositeDisposable: CompositeDisposable? = null
        private set

    init {
        mCompositeDisposable = CompositeDisposable()
    }

    override fun onCleared() {
        super.onCleared()
        mCompositeDisposable?.dispose()
    }

    open fun onDestroy() {
        onCleared()
    }
}
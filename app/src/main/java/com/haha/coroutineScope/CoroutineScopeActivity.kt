package com.haha.coroutineScope

import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityCoroutineScopeBinding
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * <pre>
 *     author : yangzy33
 *     e-mail : yangzy33@midea.com
 *     time   : 2022/10/25
 *     desc   :
 *     version: 1.0
 * </pre>
 */
class CoroutineScopeActivity :
    BaseMvvmActivity<ActivityCoroutineScopeBinding, CoroutineScopeViewModel>() {

    private val mBtnStart: Button by lazy {
        findViewById(R.id.btn_start_coroutine_scope)
    }

    private val mBtnCancel: Button by lazy {
        findViewById(R.id.btn_cancel_coroutine_scope)
    }

    private val mTvFirstScope: TextView by lazy {
        findViewById(R.id.tv_coroutine_scope_first)
    }

    private val mTvSecondScope: TextView by lazy {
        findViewById(R.id.tv_coroutine_scope_second)
    }

    private val mCoroutineTest by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinCoroutineTest()
    }

    // LeakThread 定义为 CoroutineScopeActivity 的内部类
    class LeakThread : Thread() {
        override fun run() {
            super.run()
            try {
                //线程内耗时操作
                sleep(6 * 60 * 1000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    override fun getViewModelClass(): Class<CoroutineScopeViewModel> =
        CoroutineScopeViewModel::class.java

    override fun getLayoutId(): Int = R.layout.activity_coroutine_scope

    override fun initView() {
        mBtnStart.setOnClickListener {
            mViewModel.startScan()
        }
        mBtnCancel.setOnClickListener {
            mViewModel.stopScan()
        }
        mViewModel.mSsidFirstData.observe(this) {
            mTvFirstScope.text = it
            Log.d(TAG, "mSsidFirstData: $it")
        }
        mViewModel.mSsidSecondData.observe(this) {
            mTvSecondScope.text = it
            Log.d(TAG, "mSsidSecondData: $it")
        }
        mViewModel.mediatorLiveData.observe(this) {
            Log.d(TAG, "mediatorLiveData: $it")
        }
    }

    override fun initData() {
        println("--------------------\n" + System.currentTimeMillis())
        lifecycleScope.launch {
//            eatGame()
//            test()
        }
        mCoroutineTest.test()
        println(System.currentTimeMillis())
    }

    suspend fun eatGame() {
        coroutineScope {
            val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            val game = EatGame()
            launch(dispatcher) {
                println("Ready Go!")
                delay(1000)
                game.timeout()
                println("Timeout!")
            }
            launch(dispatcher) {
                while (game.isActive) {
                    delay(60)
                    val food = Math.random()
                    println("[${Thread.currentThread().name} #1] Feed: $food >>>")
                    println("[${Thread.currentThread().name} #1] Complete: ${game.feed("$food")} >>>")
                }
            }

            launch(dispatcher) {
                while (game.isActive) {
                    delay(50)
                    println("[${Thread.currentThread().name} #2] Eat: ${game.eat()} >>>")
                }
            }
        }
    }
}
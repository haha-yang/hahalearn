package com.haha.flutter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.haha.contract.flutter.FlutterContract
import com.haha.flutter.FlutterChannel.Companion.MESSAGE_CALCULATE_SUM
import com.haha.flutter.FlutterChannel.Companion.MESSAGE_ERROR_EXCEPTION
import com.haha.flutter.FlutterChannel.Companion.MESSAGE_GET_FLUTTER_DATA
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @auther: haha
 * @Date:   2026/1/7
 * @Detail: 复用 FlutterEngineCache 中的 Engine；不负责销毁共享 Engine。
 */
class FlutterChannel(context: Context) {

    private var mAppContext: Context? = context.applicationContext

    private var mMethodChannel: MethodChannel? = null
    private var flutterJob: Job? = null

    companion object {
        private val TAG = "FlutterChannel"
        const val FLUTTER_ENGINE_ID = FlutterContract.FLUTTER_ENGINE_ID
        const val CHANNEL_NAME = FlutterContract.CHANNEL_NAME

        const val MESSAGE_CALCULATE_SUM = 101
        const val MESSAGE_GET_FLUTTER_DATA = 102
        const val MESSAGE_ERROR_EXCEPTION = 103

        val mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val obj = msg.obj
                when (msg.what) {
                    MESSAGE_CALCULATE_SUM -> {
                        println("计算总和: ${(obj as? Int) ?: 0}")
                    }

                    MESSAGE_GET_FLUTTER_DATA -> {
                        println("Flutter 返回: ${obj ?: "调用失败"}")
                    }

                    MESSAGE_ERROR_EXCEPTION -> {
                        val e = obj as? Exception
                        Log.e(TAG, "message = ${e?.message}")
                    }

                    else -> {
                        println("自定义方法结果: $obj")
                    }
                }
            }
        }

        /**
         * 从 Cache 获取或创建 Engine；仅在新建时执行 Dart 入口。
         */
        fun getOrCreateEngine(context: Context): FlutterEngine {
            val cache = FlutterEngineCache.getInstance()
            cache.get(FLUTTER_ENGINE_ID)?.let { return it }

            val engine = FlutterEngine(context.applicationContext)
            engine.dartExecutor.executeDartEntrypoint(
                DartExecutor.DartEntrypoint.createDefault()
            )
            cache.put(FLUTTER_ENGINE_ID, engine)
            return engine
        }

        /**
         * 从 Cache 移除并销毁 Engine（全局只应调用一次）。
         * 部分 Flutter Embedding 版本 remove() 返回 void，不能链式 ?.destroy()。
         */
        fun releaseCachedEngine() {
            val cache = FlutterEngineCache.getInstance()
            val engine = cache.get(FLUTTER_ENGINE_ID) ?: return
            cache.remove(FLUTTER_ENGINE_ID)
            engine.destroy()
        }
    }

    /**
     * 初始化 FlutterEngine（仅使用 applicationContext）
     */
    fun initialize() {
        val appContext = mAppContext ?: return
        val flutterEngine = getOrCreateEngine(appContext)
        mMethodChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL_NAME
        )
    }

    fun testFlutterCalls() {
        flutterJob?.cancel()
        flutterJob = CoroutineScope(Dispatchers.IO).launch {
            // 示例1：调用简单方法
            getFlutterData("Hello from Android")

            // 示例2：调用计算方法
            val numbers = listOf(1, 2, 3, 4, 5)
            calculateSum(numbers)

            // 示例3：直接调用任意方法
            callFlutterMethod(
                "customMethod",
                mapOf("key" to "value")
            )
        }
    }

    // 调用 Flutter 方法
    suspend fun callFlutterMethod(methodName: String, arguments: Any?) {
        withContext(Dispatchers.Main) {
            try {
                mMethodChannel?.invokeMethod(methodName, arguments, object : MethodChannel.Result {
                    override fun success(result: Any?) {
                        val message = mHandler.obtainMessage()
                        message.what = MessageType.getMessageValue(methodName)
                        message.obj = result
                        mHandler.sendMessage(message)
                    }

                    override fun error(
                        errorCode: String,
                        errorMessage: String?,
                        errorDetails: Any?
                    ) {
                        val message = mHandler.obtainMessage()
                        message.what = MESSAGE_ERROR_EXCEPTION
                        message.obj = Exception("$errorCode - $errorMessage")
                        mHandler.sendMessage(message)
                    }

                    override fun notImplemented() {
                        Log.i(TAG, "notImplemented")
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 获取 Flutter 数据
    suspend fun getFlutterData(input: String) {
        callFlutterMethod("getFlutterData", input)
    }

    // 计算总和
    suspend fun calculateSum(numbers: List<Int>) {
        callFlutterMethod("calculateSum", numbers)
    }

    /**
     * 仅解绑本实例资源，不销毁共享的 FlutterEngine。
     */
    fun destroy() {
        flutterJob?.cancel()
        flutterJob = null
        mHandler.removeCallbacksAndMessages(null)
        mMethodChannel?.setMethodCallHandler(null)
        mMethodChannel = null
        mAppContext = null
    }
}

enum class MessageType(val messageType: String, val value: Int) {
    GetFlutterData("getFlutterData", MESSAGE_GET_FLUTTER_DATA),
    CalculateSum("calculateSum", MESSAGE_CALCULATE_SUM),
    ErrorException("errorException", MESSAGE_ERROR_EXCEPTION);

    companion object {
        fun getMessageValue(messageType: String): Int {
            return when (messageType) {
                GetFlutterData.messageType -> GetFlutterData.value
                CalculateSum.messageType -> CalculateSum.value
                ErrorException.messageType -> ErrorException.value
                else -> ErrorException.value
            }
        }
    }
}

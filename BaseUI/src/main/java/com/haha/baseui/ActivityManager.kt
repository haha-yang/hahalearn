package com.haha.baseui

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.haha.log.DOFLogUtil
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * APP 前后台切换监听。
 */
fun interface AppForegroundListener {
    fun onAppForegroundChanged(isForeground: Boolean)
}

/**
 *     author : yangzy33
 *     time   : 2026-09-06
 *     desc   : 通过 Application.ActivityLifecycleCallbacks 管理 Activity 栈与前后台状态，
 *              替代 BaseActivity 内手动维护的 ActivityCollector。须在 Application 启动时立刻 init。
 *     version: 1.0
 */
object ActivityManager {
    private const val TAG = "ActivityManager"

    private val initialized = AtomicBoolean(false)
    private val activities = CopyOnWriteArrayList<Activity>()
    private val foregroundListeners = CopyOnWriteArrayList<AppForegroundListener>()

    @Volatile
    private var startedCount = 0

    @Volatile
    var isForeground: Boolean = false
        private set

    private var currentActivityRef: WeakReference<Activity>? = null

    /**
     * 在 [Application.attachBaseContext] / [Application.onCreate] 尽早调用，确保不漏掉任何 Activity。
     */
    fun init(application: Application) {
        if (!initialized.compareAndSet(false, true)) {
            Log.w(TAG, "already initialized")
            return
        }
        application.registerActivityLifecycleCallbacks(LifecycleCallbacks())
        Log.d(TAG, "ActivityLifecycleCallbacks registered")
    }

    fun addForegroundListener(listener: AppForegroundListener) {
        foregroundListeners.add(listener)
    }

    fun removeForegroundListener(listener: AppForegroundListener) {
        foregroundListeners.remove(listener)
    }

    fun getCurrentActivity(): Activity? = currentActivityRef?.get()

    fun getTopActivity(): Activity? =
        activities.lastOrNull { !it.isFinishing && !it.isDestroyed }

    fun getActivities(): List<Activity> = activities.toList()

    fun finishAll() {
        activities.toList().forEach { activity ->
            if (!activity.isFinishing) {
                activity.finish()
            }
        }
    }

    fun finishAllExcept(keep: Activity) {
        activities.toList().forEach { activity ->
            if (activity !== keep && !activity.isFinishing) {
                activity.finish()
            }
        }
    }

    fun finish(clazz: Class<out Activity>) {
        activities.toList().forEach { activity ->
            if (clazz.isInstance(activity) && !activity.isFinishing) {
                activity.finish()
            }
        }
    }

    private fun notifyForegroundChanged(foreground: Boolean) {
        DOFLogUtil.d(TAG, if (foreground) "onEnterForeground" else "onEnterBackground")
        foregroundListeners.forEach { listener ->
            listener.onAppForegroundChanged(foreground)
        }
    }

    private class LifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            activities.add(activity)
            DOFLogUtil.d(
                TAG,
                "onActivityCreated ${activity.javaClass.simpleName}, size=${activities.size}"
            )
        }

        override fun onActivityStarted(activity: Activity) {
            startedCount++
            if (startedCount == 1 && !isForeground) {
                isForeground = true
                notifyForegroundChanged(true)
            }
        }

        override fun onActivityResumed(activity: Activity) {
            currentActivityRef = WeakReference(activity)
        }

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) {
            startedCount = (startedCount - 1).coerceAtLeast(0)
            if (startedCount == 0 && isForeground) {
                isForeground = false
                notifyForegroundChanged(false)
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) {
            activities.remove(activity)
            if (currentActivityRef?.get() === activity) {
                currentActivityRef = null
            }
            DOFLogUtil.d(
                TAG,
                "onActivityDestroyed ${activity.javaClass.simpleName}, size=${activities.size}"
            )
        }
    }
}

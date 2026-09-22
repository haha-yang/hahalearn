package com.haha.main.timeMonitor

import android.annotation.SuppressLint
import android.content.Context

/**
 * @auther: haha
 * @Date:   2025/11/26
 * @Detail:
 */
class TimeMonitorManager {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mTimeMonitorManager: TimeMonitorManager? = null
        private var mContext: Context? = null
        private var timeMonitorList: HashMap<Int, TimeMonitor>? = null

        fun getInstance(): TimeMonitorManager {
            if (mTimeMonitorManager == null) {
                synchronized(TimeMonitorManager::class) {
                    if (mTimeMonitorManager == null) {
                        mTimeMonitorManager = TimeMonitorManager()
                    }
                }
            }
            return mTimeMonitorManager!!
        }
    }

    private constructor() {
        timeMonitorList = HashMap()
    }

    // 初始化某个打点模块
    fun resetTimeMonitor(id: Int) {
        timeMonitorList ?: return
        if (timeMonitorList?.get(id) != null) {
            timeMonitorList!!.remove(id)
        }
        getTimeMonitor(id)
    }

    // 获取打卡器
    fun getTimeMonitor(id: Int): TimeMonitor {
        var monitor = timeMonitorList?.get(id)
        if (monitor == null) {
            monitor = TimeMonitor(id)
            timeMonitorList!![id] = monitor
        }
        return monitor
    }
}
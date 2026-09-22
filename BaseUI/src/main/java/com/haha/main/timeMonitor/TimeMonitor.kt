package com.haha.main.timeMonitor

import com.haha.log.DOFLogUtil

/**
 * @auther: haha
 * @Date:   2025/11/26
 * @Detail:
 */
class TimeMonitor {
    private val TAG: String = javaClass.simpleName

    private var monitorId = -1

    // 保存一个耗时统计模块的各种耗时，tag对应某一阶段的时间
    private val mTimeTag = HashMap<String, Long>()
    private var mStartTime: Long = 0

    constructor(id: Int) {
        DOFLogUtil.d(TAG, "init TimeMonitor id: $id")
        monitorId = id
    }

    fun getMonitorId(): Int = monitorId

    fun startMonitor() {
        // 每次重新启动，都需要把前面的数据清除，避免统计到错误的数据
        if (mTimeTag.isNotEmpty()) {
            mTimeTag.clear()
        }

        mStartTime = System.currentTimeMillis()
    }

    // 打一次点，tag交线需要统计的上层自定义
    fun recodingTimeTag(tag: String) {
        // 检查是否保存过相同的tag
        if (mTimeTag.containsKey(tag)) {
            mTimeTag.remove(tag)
        }
        val time = System.currentTimeMillis() - mStartTime
        DOFLogUtil.d(TAG, "$tag: $time")
        mTimeTag[tag] = time
    }

    fun end(tag: String, writeLog: Boolean) {
        recodingTimeTag(tag)
        end(writeLog)
    }

    fun end(writeLog: Boolean) {
        if (writeLog) {
            // 写入到本地文件
        }
        testShowData();
    }

    fun testShowData() {
        if (mTimeTag.isEmpty()) {
            DOFLogUtil.e(TAG, "mTimeTag is empty!")
            return
        }

        val iterator = mTimeTag.keys.iterator()
        while (iterator.hasNext()) {
            val tag = iterator.next()
            DOFLogUtil.d(TAG, tag + ": " + mTimeTag[tag])
        }
    }

    fun getTimeTags(): HashMap<String, Long> = mTimeTag
}
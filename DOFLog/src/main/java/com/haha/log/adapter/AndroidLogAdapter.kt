package com.haha.log.adapter

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * <pre>
 *     author : yangzy33
 *     e-mail : yangzy33@midea.com
 *     time   : 2023/03/28
 *     desc   :
 *     version: 1.0
 * </pre>
 */
class AndroidLogAdapter : DefaultLogAdapter() {

    override fun log(logType: Int, modelName: String?, tag: String?, msg: String?) {
        printSplit(logType, tag, mLogStrategy?.format(modelName, tag, msg))
    }

    override fun log(logType: Int, modelName: String?, tag: String?, throwable: Throwable?) {
        log(logType, modelName, tag, mLogStrategy?.format(throwable))
    }

    override fun log(logType: Int, modelName: String?, tag: String?, json: JSONObject?) {
        log(logType, modelName, tag, mLogStrategy?.format(json))
    }

    override fun log(logType: Int, modelName: String?, tag: String?, jsonArray: JSONArray?) {
        log(logType, modelName, tag, mLogStrategy?.format(jsonArray))
    }

    override fun log(logType: Int, modelName: String?, tag: String?, map: Map<String?, Any?>?) {
        log(logType, modelName, tag, mLogStrategy?.format(map))
    }

    override fun log(logType: Int, modelName: String?, tag: String?, list: List<*>?) {
        log(logType, modelName, tag, mLogStrategy?.format(list))
    }

    private fun printSplit(logType: Int, tag: String?, msg: String?) {
        val safeTag = if (tag.isNullOrEmpty()) "DOFLog" else tag
        val content = if (msg.isNullOrEmpty()) "null" else msg
        val type = logType.coerceIn(Log.VERBOSE, Log.ASSERT)
        if (content.length <= MAX_LOG_LENGTH) {
            Log.println(type, safeTag, content)
            return
        }
        var start = 0
        while (start < content.length) {
            val end = minOf(start + MAX_LOG_LENGTH, content.length)
            Log.println(type, safeTag, content.substring(start, end))
            start = end
        }
    }

    companion object {
        private const val MAX_LOG_LENGTH = 1000
    }
}

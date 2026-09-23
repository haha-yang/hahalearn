package com.haha.service.impl.impl

import android.util.Log
import com.haha.contract.service.IUserService
import com.haha.service.annotation.IServiceLoader


/**
 *
 * @author : haha
 * @date   : 2024-09-03
 * @desc   : 测试
 * @version: 1.0
 *
 */
@IServiceLoader(interfaces = [IUserService::class], defaultImpl = true)
class UserService: IUserService {
    private val TAG = "Service"
    override fun getUserName(): String {
        return "hahanihao"
    }

    override fun start() {
        Log.d(TAG, "Loading user service")
    }
}
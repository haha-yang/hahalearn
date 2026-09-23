package com.haha.main.service

import android.content.Context
import android.os.Build
import com.haha.contract.service.IUserService
import com.haha.log.DOFLogUtil
import com.haha.service.impl.service.ServiceLoader
import com.haha.service.loader.ServiceLoaderHelper
import java.io.File
import java.io.IOException
import java.util.Locale.getDefault
import java.util.regex.Pattern

/**
 *
 * @author:     haha
 * @date:       2025/7/31
 * Description: 组件化学习
 *
 **/
object ServiceTest {
    private val TAG = javaClass.simpleName

    fun test(context: Context) {
        val iUserService = ServiceLoaderHelper.getService<IUserService>()
        DOFLogUtil.d(
            TAG,
            "iUserService == null: ${iUserService == null}, username = ${
                iUserService?.getUserName().toString()
            }"
        )
        iUserService?.start()

        val service = ServiceLoader.load(IUserService::class.java)
        DOFLogUtil.d(TAG, "dump = ${ServiceLoader.dump()}")
        val serviceLoader = service.getAll<IUserService>()
        DOFLogUtil.d(TAG, "serviceLoader.size = ${serviceLoader.size}")
        serviceLoader.forEach {
            DOFLogUtil.d(TAG, "serviceLoader = $it")
            DOFLogUtil.d(TAG, "username = ${it.getUserName()}")
            DOFLogUtil.d(TAG, "start = ${it.start()}")
        }
        testOne(context)
    }

    private fun testOne(context: Context) {
        val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        DOFLogUtil.d(TAG, "testOne: applicationInfo.sourceDir = ${applicationInfo.sourceDir}")
        val sourceApk = File(applicationInfo.sourceDir)
        val sourcePaths = ArrayList<String>()
        sourcePaths.add(applicationInfo.sourceDir)
        val extractedFilePrefix = sourceApk.name + ".classes"

        if (!isVmMutidexCapable()) {
            val totalDexNumber = context.getSharedPreferences("multidex.verson", Context.MODE_PRIVATE.or(
                Context.MODE_MULTI_PROCESS)).getInt("dex.number", 1)
            val dexDir = File(applicationInfo.dataDir, "code_cache" + File.separator + "secondary-dexes")
            for (secondaryNumber in 2 .. totalDexNumber) {
                val fileName = "$extractedFilePrefix$secondaryNumber.zip"
                val extractedFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(context.dataDir, fileName)
                } else {
                    null
                }
                if (extractedFile?.isFile == true) {
                    sourcePaths.add(extractedFile.absolutePath)
                } else {
                    throw IOException("Missing exracted secondary dex file ' ${extractedFile?.path} '")
                }
            }
        }
        DOFLogUtil.d(TAG, "testOne: sourcePaths.size = ${sourcePaths.size}")
    }

    private fun isVmMutidexCapable(): Boolean {
        var isMutidexCapable = false
        var vmName: String? = null
        try {
            if (isYunOS()) {
                vmName = "'YunOS'"
                isMutidexCapable = System.getProperty("ro.build.version.sdk").toInt() >= 12
            } else {
                vmName = "'Android'"
                val versionString = System.getProperty("java.vm.version")
                if (versionString != null) {
                    val matcher = Pattern.compile("(\\d+).(\\d+).(\\.\\d+)?").matcher(versionString)
                    if (matcher.matches()) {
                        try {
                            val major = matcher.group(1)?.toIntOrNull() ?: 0
                            val minor = matcher.group(2)?.toIntOrNull() ?: 0
                            isMutidexCapable = major >= 2 || minor >= 1
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        DOFLogUtil.i(
            TAG,
            "isVmMutidexCapable: VM with name: $vmName, ${if (isMutidexCapable) " has multidex support" else " does not have multidex support"}"
        )
        return isMutidexCapable
    }

    private fun isYunOS(): Boolean {
        try {
            val version = System.getProperty("ro.yunos.version")
            val vmName = System.getProperty("java.vm.name")
            return (vmName != null && vmName.lowercase(getDefault()).contains("lemur"))
                    || (version != null && version.trim().isNotEmpty())
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
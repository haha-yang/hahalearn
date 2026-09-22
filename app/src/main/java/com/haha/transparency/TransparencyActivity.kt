package com.haha.transparency

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.view.Window
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityTransparencyBinding

class TransparencyActivity : BaseMvvmActivity<ActivityTransparencyBinding, BaseViewModel>() {

    private val isHandlePermission: Boolean = false
    override fun getLayoutId(): Int = R.layout.activity_transparency

    override fun initView() {
//        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val permissions = intent.getStringArrayExtra("permissions") as Array<String>?
        requestPermissions(permissions)
    }

    override fun requestFeature() {
        super.requestFeature()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    override fun initData() {
    }

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    private fun requestPermissions(permissions: Array<String>?) {
        if (permissions == null) return
        val needRequestPermissions = arrayListOf<String>()
        for (permission in permissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val isGranted = ContextCompat.checkSelfPermission(this, packageName) == PackageManager.PERMISSION_GRANTED
                if (!isGranted) {
                    needRequestPermissions.add(permission)
                }
            }
        }

        val needPermissionArray = Array(needRequestPermissions.size) { k -> needRequestPermissions[k] }
        ActivityCompat.requestPermissions(
            this, needPermissionArray,
            200
        )
    }

    fun getAppOps(context: Context): Boolean {
        try {
            val appOpsManager = context.getSystemService(APP_OPS_SERVICE) as AppOpsManager
            val checkResult = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_FINE_LOCATION, Binder.getCallingUid(), context.packageName)
            val checkResult1 = appOpsManager.checkOp(AppOpsManager.OPSTR_FINE_LOCATION, Binder.getCallingUid(), context.packageName)
            val `object` = context.getSystemService(Context.APP_OPS_SERVICE) ?: return false
            val localClass: Class<*> = `object`.javaClass
            val arrayOfClass: Array<Class<*>?> = arrayOfNulls(3)
            arrayOfClass[0] = Integer.TYPE
            arrayOfClass[1] = Integer.TYPE
            arrayOfClass[2] = String::class.java
            val method = localClass.getMethod("checkOp", *arrayOfClass) ?: return false
            val arrayOfObject1 = arrayOfNulls<Any>(3)
            arrayOfObject1[0] = Integer.valueOf(24)
            arrayOfObject1[1] = Integer.valueOf(Binder.getCallingUid())
            arrayOfObject1[2] = context.packageName
            val m = (method.invoke(`object`, *arrayOfObject1) as Int).toInt()
            return m == AppOpsManager.MODE_ALLOWED
        } catch (ex: Exception) {
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            200 -> {
                for (i in grantResults.indices) {
                    val isShow = getAppOps(this)

                    // 权限被拒绝并不再咨询
                    val noConsulted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            shouldShowRequestPermissionRationale(permissions[i])

                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED && !noConsulted && !isShow) {
                        val intent = Intent()
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                        intent.data = Uri.fromParts("package", packageName, null)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "haha", Toast.LENGTH_SHORT).show()
                    }
                }
                finish()
            }
            else -> {

            }
        }
    }
}
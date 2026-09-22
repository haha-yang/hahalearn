package com.haha.gps

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityGpsBinding
import com.haha.router.RoutePath
import com.haha.servicerouterannotation.annotation.Route

@Route(path = RoutePath.GPS, name = "Gps")
class GpsActivity : BaseMvvmActivity<ActivityGpsBinding, BaseViewModel>() {

    companion object {
        const val ACCESS_FIND_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        const val ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    }

    private val mTvLocation: TextView by lazy {
        findViewById(R.id.text_gps)
    }
    private val mBtnLocationShow: Button by lazy {
        findViewById(R.id.btn_gps_show)
    }
    private val mBtnLocationReset: Button by lazy {
        findViewById(R.id.btn_gps_reset)
    }
    private val mBtnCoarseLocation: Button by lazy {
        findViewById(R.id.btn_coarse_location)
    }
    private val mBtnFindLocation: Button by lazy {
        findViewById(R.id.btn_find_location)
    }
    private val mTvConnectWifiInfo: TextView by lazy {
        findViewById(R.id.tv_connect_wifi)
    }
    private val mBtnRefreshWifi: TextView by lazy {
        findViewById(R.id.btn_refresh_wifi)
    }

    private val mWifiManager: WifiManager by lazy {
        applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
    }

    private var gpsLocationManager: GPSLocationManager? = null

    /**
     * 独立监听对象；配合 Manager.stop() + WeakReference，避免单例间接持有 Activity
     */
    private val locationListener = object : GPSLocationListener {
        override fun updateLocation(location: Location?) {
            runOnUiThread { showLocation(location) }
        }

        override fun updateStatus(provider: String?, status: Int, extras: Bundle?) {
            // 状态变化暂不展示
        }

        override fun updateGPSProviderStatus(gpsStatus: Int) {
            when (gpsStatus) {
                GPSProviderStatus.GPS_DISABLED -> runOnUiThread { showLocation(null) }
                GPSProviderStatus.GPS_ENABLED -> runOnUiThread {
                    // provider 重新可用时尝试刷新一次
                    startLocationIfPermitted(forceOpenGps = false)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        gpsLocationManager = GPSLocationManager.getInstances(this)
        val manager = gpsLocationManager ?: return
        if (manager.isGpsAble()) {
            startLocationIfPermitted(forceOpenGps = false)
        } else {
            manager.openGPS()
        }
    }

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun getLayoutId(): Int {
        return R.layout.activity_gps
    }

    override fun initView() {
        mBtnLocationReset.setOnClickListener {
            mTvLocation.text = "默认定位信息"
        }
        mBtnLocationShow.setOnClickListener {
            startLocationIfPermitted(forceOpenGps = false)
        }
        mBtnCoarseLocation.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                AlertDialog.Builder(this).apply {
                    setTitle("请求模糊定位权限")
                    setMessage("获取位置经纬度需要获取模糊权限")
                    setPositiveButton("同意") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this@GpsActivity,
                            arrayOf(ACCESS_COARSE_LOCATION),
                            200
                        )
                    }
                    setNegativeButton("拒绝", null)
                }.show()
            } else {
                Toast.makeText(this, "模糊定位权限已经开启", Toast.LENGTH_SHORT).show()
            }
        }
        mBtnFindLocation.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, ACCESS_FIND_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                AlertDialog.Builder(this).apply {
                    setTitle("请求GPS精准定位")
                    setMessage("获取位置经纬度需要获取GPS精准权限")
                    setPositiveButton("同意") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this@GpsActivity,
                            arrayOf(ACCESS_FIND_LOCATION),
                            200
                        )
                    }
                    setNegativeButton("拒绝", null)
                }.show()
            } else {
                Toast.makeText(this, "精准定位权限已经开启", Toast.LENGTH_SHORT).show()
            }
        }
        mBtnRefreshWifi.setOnClickListener {
            val scanResult = mWifiManager.scanResults
            val connectInfo = mWifiManager.connectionInfo
            mTvConnectWifiInfo.text =
                if (scanResult.isNotEmpty()) {
                    "${connectInfo.ssid}\t 搜索到wifi数：${scanResult.size}"
                } else {
                    "无法获取wifi信息"
                }
        }
    }

    override fun initData() {
        startLocationIfPermitted(forceOpenGps = false)
    }

    private fun startLocationIfPermitted(forceOpenGps: Boolean) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "定位权限未开启", Toast.LENGTH_SHORT).show()
            return
        }
        val manager = gpsLocationManager ?: GPSLocationManager.getInstances(this).also {
            gpsLocationManager = it
        }
        manager.setScanSpan(1000)
        manager.setMinDistance(0f)
        manager.start(locationListener, forceOpenGps)
    }

    private fun showLocation(location: Location?) {
        if (location == null) {
            mTvLocation.text = "无法获取"
        } else {
            mTvLocation.text =
                "latitude = ${location.latitude}, longitude = ${location.longitude}"
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            200 -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    startLocationIfPermitted(forceOpenGps = false)
                } else {
                    val intent = Intent().apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onPause() {
        gpsLocationManager?.stop()
        super.onPause()
    }

    override fun onDestroy() {
        gpsLocationManager?.stop()
        gpsLocationManager = null
        super.onDestroy()
    }
}

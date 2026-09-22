package com.haha.network

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.lifecycle.viewModelScope
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityNetworkBinding
import com.haha.network.encrypt.Encryption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetworkActivity : BaseMvvmActivity<ActivityNetworkBinding, BaseViewModel>() {
    private val mWebView: WebView by lazy {
        findViewById(R.id.web_view)
    }

    private var mWiFiNetworkMonitor: WiFiNetworkMonitor? = null

    private val mWifiManager: WifiManager by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val mWifiReceiver: BroadcastReceiver by lazy(LazyThreadSafetyMode.PUBLICATION) {
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION == intent?.action) {
                    // 获取扫描结果
                    val scanResults: MutableList<ScanResult> = mWifiManager.scanResults
                    // 处理扫描结果
                    for (result in scanResults) {
                        Log.d(
                            "WifiScan",
                            "SSID: ${result.SSID}, BSSID: ${result.BSSID}, 强度: ${result.level}," +
                                    " wifiSSid: ${result.wifiSsid}, wifiStandard: ${result.wifiStandard}"
                        )
                        if ("liujiiang".equals(result.SSID, true)) {
                            mWiFiNetworkMonitor?.connectWiFi(result, "15626000989!", object :
                                WiFiNetworkMonitor.WiFiConnectCallback {
                                /**
                                 * Connect success.
                                 */
                                override fun onConnectSuccess() {
                                    Log.d(TAG, "onConnectSuccess")
                                }

                                /**
                                 * Connect failed.
                                 *
                                 * @param errorCode    errorCode
                                 * @param errorMessage errorMessage
                                 * @see .ERR_CONNECT_FAILED
                                 *
                                 * @see .ERR_CONNECT_TIMEOUT
                                 *
                                 * @see .ERR_PASSWORD_WRONG
                                 */
                                override fun onConnectFailed(
                                    errorCode: Int,
                                    errorMessage: String?
                                ) {
                                    Log.e(TAG, "errorCode: $errorCode, errorMessage: $errorMessage")
                                }

                            })
                        }
                    }
                }
            }
        }
    }

    override fun requestPermissionArray(): Array<String> {
        return arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun handlePermissionResult(permissionResultMap: Map<String, Boolean>) {
        permissionResultMap.forEach {
            if (it.key != Manifest.permission.ACCESS_FINE_LOCATION || !it.value) {
                Log.e(TAG, "permission ${Manifest.permission.ACCESS_FINE_LOCATION} denied")
                finish()
            }
        }
    }

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun getLayoutId(): Int = R.layout.activity_network

    override fun initView() {
        mWebView.webViewClient = object: WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                url: String?
            ): Boolean {
                url?.let{
                    try {
                        if(it.startsWith("http") || it.startsWith("https")) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                            return true
                        }
                    } catch (e: Exception) {
                        return true
                    }
                    mWebView.loadUrl(url)
                    return true
                }
                return false
            }
        }
        mWebView.settings.javaScriptEnabled = true
        mWebView.loadUrl("https://www.baidu.com")

        findViewById<Button>(R.id.btn_scan_wifi).setOnClickListener {
            mWifiManager.startScan()
        }
    }

    override fun initData() {
        mWiFiNetworkMonitor = WiFiNetworkMonitor(mContext.applicationContext)

        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(mWifiReceiver, intentFilter)
        val encryption = Encryption()

        mViewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                encryption.init()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mWifiReceiver)
        mWiFiNetworkMonitor = null
    }
}
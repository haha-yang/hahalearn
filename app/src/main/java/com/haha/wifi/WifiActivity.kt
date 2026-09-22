package com.haha.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityWifiBinding
import com.haha.log.DOFLogUtil
import com.haha.util.PermissionUtils


/**
 * <pre>
 *     author : yangzy33
 *     e-mail : yangzy33@midea.com
 *     time   : 2023/03/27
 *     desc   :
 *     version: 1.0
 * </pre>
 */
class WifiActivity: BaseMvvmActivity<ActivityWifiBinding, BaseViewModel>() {

    private var mTvConnectWifi: TextView? = null

    private var mTvRefresh: TextView? = null

    private var mRv: RecyclerView? = null

    private var mWifiAdapter: WifiAdapter? = null

    private var mWifiReceiver: WifiReceiver? = null
    override fun getLayoutId(): Int = R.layout.activity_wifi

    override fun initView() {
        mTvConnectWifi = findViewById(R.id.tv_connect_wifi)
        mTvRefresh = findViewById(R.id.tv_refresh)
        mTvRefresh?.setOnClickListener {
            startWifiScan()
        }

        mRv = findViewById(R.id.rv_list_wifi)
    }

    override fun initData() {
        addReceiver()
        startWifiScan()
    }

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun onDestroy() {
        unregisterReceiver(mWifiReceiver)
        super.onDestroy()
    }

    private fun startWifiScan() {
        if (!PermissionUtils.checkGPSPermission()) {
            Toast.makeText(this, "GPS权限没有开启", Toast.LENGTH_SHORT).show()
            return
        }
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val resultWifi = wifiManager.scanResults
        mWifiAdapter = WifiAdapter(this, resultWifi)
        val linearLayoutManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        mRv?.layoutManager = linearLayoutManager
        mRv?.adapter = mWifiAdapter
        mWifiAdapter?.notifyDataSetChanged()
    }

    private fun addReceiver() {
        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION)
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION)

        mWifiReceiver = WifiReceiver()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mWifiReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(mWifiReceiver, filter)
        }
    }
}

/*
    ConnectivityManager.CONNECTIVITY_ACTION
    WifiManager.WIFI_STATE_CHANGED_ACTION
    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
    WifiManager.NETWORK_IDS_CHANGED_ACTION
    WifiManager.SUPPLICANT_STATE_CHANGED_ACTION
    WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION
    WifiManager.LINK_CONFIGURATION_CHANGED_ACTION
    WifiManager.NETWORK_STATE_CHANGED_ACTION
    WifiManager.RSSI_CHANGED_ACTION
 */
class WifiReceiver: BroadcastReceiver() {
    val TAG = this.javaClass.name

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        DOFLogUtil.d(TAG, "action = ${intent.action}")
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                val extraNoConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, true)

            }

            /**
             * 其中:
            0 --> WiFiManager.WIFI_STATE_DISABLING, 表示 WiFi 正关闭的瞬间状态;
            1 --> WifiManager.WIFI_STATE_DISABLED, 表示 WiFi 模块已经完全关闭的状态;
            2 --> WifiManager.WIFI_STATE_ENABLING, 表示 WiFi 模块正在打开中瞬间的状态;
            3 --> WiFiManager.WIFI_STATE_ENABLED, 表示 WiFi 模块已经完全开启的状态;
            4 --> WiFiManager.WIFI_STATE_UNKNOWN, 表示 WiFi 处于一种未知状态;
            通常是在开启或关闭WiFi的过程中出现不可预知的错误, 通常是底层状态机可能跑的出现故障了, 会到这种情况, 与底层控制相关;
             */
            WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                when (intent.getIntExtra("EXTRA_WIFI_STATE", WifiManager.WIFI_STATE_UNKNOWN)) {
                    WifiManager.WIFI_STATE_ENABLED -> {

                    }
                }
            }
        }
    }

}

class WifiAdapter(val context: Context, val wifiList: MutableList<ScanResult>): Adapter<WifiAdapter.WifiViewHolder>() {

    private var mList: MutableList<ScanResult>? = mutableListOf<ScanResult>()

    override fun getItemCount(): Int = mList?.size ?: 0

    init {
        mList?.clear()
        mList?.addAll(wifiList)
    }

    override fun onBindViewHolder(holder: WifiViewHolder, position: Int) {
        if (mList.isNullOrEmpty()) return
        holder.setScnResult(mList!![position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_wifi, null)
        return WifiViewHolder(context, view)
    }

    class WifiViewHolder: ViewHolder {
        private var mLlWifi: RelativeLayout? = null
        private var mTvWifiName: TextView? = null
        private var mTvWifiSignal: TextView? = null
        private var mContext: Context? = null
        private var mEtPassword: EditText? = null

        private var mScanResult: ScanResult? = null

        constructor(context: Context?, view: View) : super(view) {
            mContext = context
            mLlWifi = view.findViewById(R.id.rl_wifi)
            mTvWifiName = view.findViewById(R.id.tv_wifi_name)
            mTvWifiSignal = view.findViewById(R.id.tv_wifi_signal)
//            mEtPassword = view.findViewById(R.id.et_password)

            mLlWifi?.setOnClickListener {
                val wifiManager = mContext?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager?
                wifiManager?.let {
                    if (ActivityCompat.checkSelfPermission(mContext!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@setOnClickListener
                    }
                    val alreadyWifiList = it.configuredNetworks
                    var flag = true
                    mScanResult?.let { scanResult ->
                        for (configuredNetwork in alreadyWifiList) {
                            if (scanResult.SSID == configuredNetwork.SSID.replace("\"", "")) {
                                flag = false
                                val config = WifiConfiguration()
                                config.SSID = "\"" +scanResult.SSID +"\""
                                config.preSharedKey = "\""+configuredNetwork.preSharedKey+"\"";//加密wifi
                                config.hiddenSSID = true;
                                config.status = WifiConfiguration.Status.ENABLED;
                                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);//WPA_PSK  NONE（非加密）
                                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                                val netId = it.addNetwork(config)
                                val b = it.enableNetwork(netId, true)
                                break
                            }
                        }

                        if (flag){
                            //没有找到相同配置
                            val password = mEtPassword?.text?.toString()
                            if (password.isNullOrEmpty()){
                                Toast.makeText(mContext,"请输入密码", Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
                            val config = WifiConfiguration()
                            config.SSID = "\""+scanResult.SSID+"\""
                            config.preSharedKey = "\""+password+"\"" //加密wifi
                            config.hiddenSSID = true
                            config.status = WifiConfiguration.Status.ENABLED
                            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
                            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE) //WPA_PSK  NONE（非加密）
                            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                            val netId = it.addNetwork(config)
                            val b = it.enableNetwork(netId, true)
                        }
                    }
                }
            }
        }

        fun setScnResult(scanResult: ScanResult?) {
            this.mScanResult = scanResult
            this.mTvWifiName?.text = scanResult?.SSID ?: ""
            this.mTvWifiSignal?.text = scanResult?.frequency.toString()
        }
    }
}
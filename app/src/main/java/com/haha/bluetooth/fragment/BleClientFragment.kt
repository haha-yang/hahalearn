package com.haha.bluetooth.fragment

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haha.baseui.mvvm.BaseMvvmFragment
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.blesdk.bean.BleScanResult
import com.haha.blesdk.communication.BleClientManager
import com.haha.blesdk.constants.BleConstants
import com.haha.blesdk.exceptions.BleErrorException
import com.haha.blesdk.interfaces.BleGattCallback
import com.haha.blesdk.interfaces.IBleScanCallback
import com.haha.blesdk.manager.BleScanManager
import com.haha.blesdk.scan.BaseBleScanDevice
import com.haha.blesdk.scan.LowPowerBleScanDevice
import com.haha.bluetooth.adapter.BleScanAdapterV2
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.FragmentBleClientBinding


/**
 *     author : haha
 *     time   : 2024-08-21
 *     desc   :
 *     version: 1.0
 */
@SuppressWarnings("missingPermission")
class BleClientFragment : BaseMvvmFragment<FragmentBleClientBinding, BaseViewModel>() {

    companion object {
        private val handler = Handler(Looper.getMainLooper())
    }
    private var mBleAdapter: BleScanAdapterV2? = null
    private val mData: MutableList<BleScanResult> = mutableListOf();
    private var mBluetoothGatt: BluetoothGatt? = null
    private val mSb = StringBuilder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var blueGatt: BluetoothGatt? = null
    private var isConnected = false
    private var blueGattListener: BleGattCallback? = object : BleGattCallback() {
        override fun onObtainGattServiceStatus(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, "onObtainGattServiceStatus: status = $status")
        }

        override fun onReadOrWriteException(
            gatt: BluetoothGatt?,
            bleException: BleErrorException?
        ) {
            Log.d(TAG, "onReadOrWriteException: bleException = ${bleException?.message}")
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val device = gatt?.device
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true
                //开始发现服务，有个小延时，最后200ms后尝试发现服务
                handler.postDelayed({
                    gatt?.discoverServices()
                }, 300)

                device?.let { logInfo("与 ${it.name} 连接成功!!!") }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false
                logInfo("无法与 ${device?.name} 连接: $status")
                closeConnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val service = gatt?.getService(BleConstants.UUID_SERVICE)
            mBluetoothGatt = gatt
            logInfo("已连接上 GATT 服务，可以通信! ")

            /*if (status == BluetoothGatt.GATT_SUCCESS){
                gatt?.services?.forEach {service ->
                    logInfo("service 的 uuid: ${service.uuid}")
                    service.characteristics.forEach{ characteristic ->
                        logInfo("characteristic 的 uuid: ${characteristic.uuid}")
                        characteristic.descriptors.forEach { descrip ->
                            logInfo("descrip 的 uuid: ${descrip.uuid}")
                        }
                    }
                }
            }*/
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            characteristic?.let {
                val data = String(it.value)
                logInfo("CharacteristicRead 数据: $data")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            characteristic?.let {
                val data = String(it.value)
                logInfo("CharacteristicWrite 数据: $data")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.let {
                val data = String(it.value)
                logInfo("CharacteristicChanged 数据: $data")
            }
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            descriptor?.let {
                val data = String(it.value)
                logInfo("DescriptorRead 数据: $data")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            descriptor?.let {
                val data = String(it.value)
                logInfo("DescriptorWrite 数据: $data")
            }
        }
    }
    private var mScanBleDevice: BaseBleScanDevice? = null

    private var mBleClientManager: BleClientManager? = null

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_ble_client

    override fun isUsedEncapsulatedPermissions(): Boolean {
        return true
    }

    override fun requestPermissionArray(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    override fun handlePermissionResult(permissionResultMap: Map<String, Boolean>) {
        permissionResultMap.forEach { (k, v) ->
            Log.d(TAG, "$k ----->>>>>  $v")
        }
    }

    override fun initView() {
        mViewDataBinding.scan.setOnClickListener { scan() }
        mViewDataBinding.readData.setOnClickListener { mBleClientManager?.readBleMessage() }
        mViewDataBinding.writeData.setOnClickListener {
            val message = mViewDataBinding.edit.text.toString()
            mBleClientManager?.writeBleMessage(message)
        }
        initRecyclerView()
        // TODO 这儿逻辑待定，暂时先注释掉
//        handler.postDelayed()
    }

    /**
     * 扫描
     */
    private fun scan() {
        mData.clear()
        mBleAdapter?.notifyDataSetChanged()
        mScanBleDevice = LowPowerBleScanDevice()
        mScanBleDevice!!.apply {
            //扫描设置
            val scanSettings = ScanSettings.Builder()
                /**
                 * 三种模式
                 * - SCAN_MODE_LOW_POWER : 低功耗模式，默认此模式，如果应用不在前台，则强制此模式
                 * - SCAN_MODE_BALANCED ： 平衡模式，一定频率下返回结果
                 * - SCAN_MODE_LOW_LATENCY 高功耗模式，建议应用在前台才使用此模式
                 */
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)//高功耗，应用在前台

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                /**
                 * 三种回调模式
                 * - CALLBACK_TYPE_ALL_MATCHED : 寻找符合过滤条件的广播，如果没有，则返回全部广播
                 * - CALLBACK_TYPE_FIRST_MATCH : 仅筛选匹配第一个广播包出发结果回调的
                 * - CALLBACK_TYPE_MATCH_LOST : 这个看英文文档吧，不满足第一个条件的时候，不好解释
                 */
                scanSettings.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            }

            //判断手机蓝牙芯片是否支持皮批处理扫描
            if (getBluetoothAdapter()?.isOffloadedFilteringSupported == true) {
                scanSettings.setReportDelay(0L)
            }

            setCallback(TAG, object : IBleScanCallback {
                override fun onBatchScanResults(results: MutableList<BleScanResult>?) {
                    Log.d(TAG, "onBatchScanResults --->>>  results.size() = ${results?.size}")
                }

                override fun onScanResult(callbackType: Int, bleScanResult: BleScanResult?) {
                    Log.d(TAG, "onScanResult --->>>  bleScanResult = ${bleScanResult}")
                    bleScanResult?.let {
                        it.name ?: return
                        if (it.name!!.startsWith("colmo", true) ||
                            it.name!!.startsWith("midea", true)
                        ) {
                            return@let
                        }
                        if (it !in mData) {
                            mData.add(it)
                            mBleAdapter?.notifyItemInserted(mData.size)
                        }
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.d(TAG, "onScanFailed --->>>  errorCode = ${errorCode}")
                }

            })
            (this as? LowPowerBleScanDevice)?.startScan(TAG, 3000L, null, scanSettings.build())
        }
    }

    override fun initData() {
        //是否支持低功耗蓝牙
        initBluetooth()
        bluetoothAdapter = BleScanManager.getInstance()?.getBluetoothAdapter()
        mBleClientManager = BleClientManager()
    }

    /**
     * 初始化 recyclerview
     */
    private fun initRecyclerView() {
        val recyclerView: RecyclerView = mViewDataBinding.recycler
        val manager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = manager
        mBleAdapter = BleScanAdapterV2(mData, R.layout.adapter_item_ble_scan).apply {
            setOnItemClickListener { _, _, position ->
                //连接之前先关闭连接
                closeConnect()
                val bleData = mData[position]
                Log.d(TAG, "setOnItemClickListener: bleData =\n$bleData")
                mBleClientManager?.connect(
                    bleData.device,
                    requireContext(),
                    false,
                    blueGattListener
                )
                logInfo("开始与 ${bleData.name} 连接.... $blueGatt")
            }
        }
        recyclerView.adapter = mBleAdapter
    }

    private fun initBluetooth() {
        requireActivity().packageManager.takeIf { !it.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }
            ?.let {
                Toast.makeText(requireContext(), "您的设备没有低功耗蓝牙驱动！", Toast.LENGTH_SHORT)
                    .show()
                requireActivity().finish()
            }
        bluetoothAdapter = BleScanManager.getInstance()?.getBluetoothAdapter()
    }

    /**
     * 断开连接
     */
    private fun closeConnect() {
        mScanBleDevice?.stopScan(TAG)
        mBleClientManager?.closeConnect()
        mBleClientManager = null
        blueGattListener = null
        mBleAdapter?.setOnItemClickListener(null)
    }

    private fun logInfo(msg: String) {
        Log.d(TAG, "logInfo = ${mSb.apply { append(msg).append("\n") }}")
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        closeConnect()
        super.onDestroy()
    }
}
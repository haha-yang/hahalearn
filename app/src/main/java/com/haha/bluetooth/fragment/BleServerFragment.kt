package com.haha.bluetooth.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.haha.baseui.mvvm.BaseMvvmFragment
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.blesdk.communication.BleServerImpl
import com.haha.blesdk.constants.BleConstants
import com.haha.blesdk.manager.BleScanManager
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.FragmentBleServerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 *     author : yangzy33
 *     time   : 2024-08-21
 *     desc   :
 *     version: 1.0
 */
class BleServerFragment : BaseMvvmFragment<FragmentBleServerBinding, BaseViewModel>() {

    private val mSb = StringBuilder()
    private var bluetoothAdapter: BluetoothAdapter? = null

    @SuppressWarnings("missingPermission")
    private var gattServiceCallback: BluetoothGattServerCallback? = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            device ?: return
            Log.d(TAG, "zsr onConnectionStateChange: ")
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                logInfo("连接到中心设备: ${device.name}")
            } else {
                logInfo("与: ${device.name} 断开连接失败！")
            }
        }


        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            /**
             * 中心设备read时，回调
             */
            val data = "this is a test from ble server"
            logInfo("客户端读取 [characteristic ${characteristic?.uuid}] $data")
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            value?.let {
                logInfo("客户端写入 [characteristic ${characteristic?.uuid}] ${String(it)}")
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            val data = "this is a test"
            logInfo("客户端读取 [descriptor ${descriptor?.uuid}] $data")
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            value?.let {
                logInfo("客户端写入 [descriptor ${descriptor?.uuid}] ${String(it)}")
                // 简单模拟通知客户端Characteristic变化
                Log.d(TAG, "zsr onDescriptorWriteRequest: $value")
            }
        }

        override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
            Log.d(TAG, "zsr onExecuteWrite: ")
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            Log.d(TAG, "zsr onNotificationSent: ")
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            Log.d(TAG, "zsr onMtuChanged: ")
        }
    }

    private var mBleServerImpl: BleServerImpl? = null

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_ble_server

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

    @SuppressLint("MissingPermission")
    override fun handlePermissionResult(permissionResultMap: Map<String, Boolean>) {
        var isAllGrant = true
        permissionResultMap.forEach { (k, v) ->
            Log.d(TAG, "$k ----->>>>>  $v")
            isAllGrant = isAllGrant.and(v)
        }
        if (isAllGrant) {
            initBle()
        }
    }

    override fun initView() {
    }

    override fun initData() {
    }

    @SuppressLint("MissingPermission")
    private fun initBle() {
        bluetoothAdapter = BleScanManager.getInstance()?.getBluetoothAdapter()
        bluetoothAdapter?.name = "k20"
        val bleManager = BleScanManager.getInstance()?.getBluetoothManager()
        if (bluetoothAdapter != null && bleManager != null) {
            mBleServerImpl = BleServerImpl(requireContext(), bluetoothAdapter!!, bleManager)
        }
        mBleServerImpl?.apply {
            setGattServiceCallback(TAG, gattServiceCallback)
            createAndAddBleService(
                true,
                BleConstants.UUID_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY,
                BleConstants.UUID_READ_NOTIFY,
                BleConstants.UUID_WRITE,
                BleConstants.UUID_DESCRIBE
            )
        }
    }

    private fun logInfo(msg: String) {
        Log.d(TAG, "logInfo = ${mSb.apply { append(msg).append("\n") }}")
        mViewModel.viewModelScope.launch(Dispatchers.Main) {
            mViewDataBinding.info.text = mSb.toString()
        }
    }

    @SuppressWarnings("missingPermission")
    override fun onDestroy() {
        super.onDestroy()
        mBleServerImpl?.close(TAG)
        gattServiceCallback = null
    }
}
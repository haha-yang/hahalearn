package com.haha.bluetooth.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.haha.baseui.mvvm.BaseMvvmFragment
import com.haha.blesdk.bean.BleScanResult
import com.haha.blesdk.bean.toBleScanResult
import com.haha.blesdk.constants.BleScanSettings
import com.haha.blesdk.interfaces.IBleScanCallback
import com.haha.blesdk.manager.BleBondManager
import com.haha.blesdk.permissions.BleSDkPermissionManager
import com.haha.blesdk.scan.BaseBleScanDevice
import com.haha.blesdk.scan.ClassicBleScanDevice
import com.haha.bluetooth.BleViewModel
import com.haha.bluetooth.adapter.BleBondedAdapter
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.FragmentBleBondedBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 *     author : haha
 *     time   : 2024-08-13
 *     desc   :
 *     version: 1.0
 */
class BleBondedFragment: BaseMvvmFragment<FragmentBleBondedBinding, BleViewModel>() {

    private var mBondBleScanAdapter: BleBondedAdapter? = null
    private var mBondBleDevices = mutableListOf<BleScanResult>()
    private var mBleScanDevice: BaseBleScanDevice? = null

    override fun getViewModelClass(): Class<BleViewModel> = BleViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_ble_bonded

    override fun isUsedEncapsulatedPermissions(): Boolean = true

    override fun requestPermissionArray(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
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
        mBondBleScanAdapter = BleBondedAdapter(mBondBleDevices, R.layout.adapter_item_ble_bonded).apply {
            setCallback(object : BleBondedAdapter.ItemClickCallback {

                override fun onClickUnbind(result: BleScanResult) {
                    BleBondManager.removeBond(result)
                }

                override fun onClickCommunicate(result: BleScanResult) {
                    mBleScanDevice?.stopScan(TAG)
                    mViewModel.mBleRfcommConnectManager?.connect(result)
                }
            })
        }
        mViewDataBinding.tvBluetooth.apply {
            val tvBluetooth = findViewById<TextView>(R.id.tv_bluetooth)
            tvBluetooth.text = "蓝牙绑定"
            tvBluetooth.setOnClickListener {
                mViewModel.mBleCommunicateManager?.sendMessage("蓝牙图标")
            }
        }
        mViewDataBinding.rvBondedBle.let {
            it.adapter = mBondBleScanAdapter
            val layoutManager = LinearLayoutManager(mContext).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            it.layoutManager = layoutManager
        }
    }

    override fun initData() {
        mBleScanDevice = ClassicBleScanDevice()
        mBleScanDevice!!.apply {
            addFilter(BluetoothDevice.ACTION_FOUND)
            addFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            getBluetoothReceiver()?.setBleBoundCallback(object : IBleScanCallback {
                override fun onBatchScanResults(results: MutableList<BleScanResult>?) {

                }

                override fun onScanResult(callbackType: Int, bleScanResult: BleScanResult?) {
                    bleScanResult ?: return
                    if (callbackType == BleScanSettings.CALLBACK_TYPE_REMOVE_BOUND_DEVICE.callbackType) {
                        var i = 0
                        while (i < mBondBleDevices.size) {
                            if (mBondBleDevices[i].mac == bleScanResult.mac) {
                                break
                            } else {
                                i ++
                            }
                        }
                        if (i < mBondBleDevices.size) {
                            mBondBleDevices.removeAt(i)
                            mViewModel.viewModelScope.launch(Dispatchers.Main) {
                                mBondBleScanAdapter?.notifyItemRemoved(i)
                            }
                        }
                    } else {
                        mBondBleDevices.add(bleScanResult)
                        mViewModel.viewModelScope.launch(Dispatchers.Main) {
                            mBondBleScanAdapter?.notifyItemChanged(mBondBleDevices.size)
                        }
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                }

            })
            registerBleReceiver(requireActivity())
        }
        obtainBondedDevices()
    }

    @SuppressLint("MissingPermission")
    private fun obtainBondedDevices() {
        if (BleSDkPermissionManager.isGrantConnectRelatedPermissions()) {
            mBleScanDevice?.getBleBondedDevices()?.let {
                for (device in it) {
                    mBondBleDevices.add(device.toBleScanResult())
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBleScanDevice?.unregisterBleReceiver(requireActivity())
        mViewModel.close()
    }
}
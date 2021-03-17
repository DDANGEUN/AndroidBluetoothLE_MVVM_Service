package com.lilly.ble.viewmodel

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.*
import com.lilly.ble.*
import com.lilly.ble.util.Event
import kotlinx.coroutines.Dispatchers
import java.util.*
import kotlin.concurrent.schedule


class BleViewModel(private val myRepository: MyRepository) : ViewModel() {

    val TAG = "MainViewModel"

    val statusTxt: LiveData<String> = myRepository.fetchStatusText.asLiveData(viewModelScope.coroutineContext)

    val readTxt: LiveData<String>? = myRepository.readDataFlow


    val _isConnect : LiveData<Event<Boolean>>
       get() = myRepository.isConnected

    // ble manager
    val bleManager: BluetoothManager =
        MyApplication.applicationContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    // ble adapter
    val bleAdapter: BluetoothAdapter?
        get() = bleManager.adapter


    private val _requestEnableBLE = MutableLiveData<Event<Boolean>>()
    val requestEnableBLE : LiveData<Event<Boolean>>
        get() = _requestEnableBLE

    private val _listUpdate = MutableLiveData<Event<ArrayList<BluetoothDevice>?>>()
    val listUpdate : LiveData<Event<ArrayList<BluetoothDevice>?>>
        get() = _listUpdate



    var isScanning = ObservableBoolean(false)
    var isConnect = ObservableBoolean(false)

    // scan results
    var scanResults: ArrayList<BluetoothDevice>? = ArrayList()



    /**
     *  Start BLE Scan
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun onClickScan(){
        startScan()
    }

    fun startScan() {
        // check ble adapter and ble enabled
        if (bleAdapter == null || !bleAdapter?.isEnabled!!) {
            _requestEnableBLE.postValue(Event(true))
            myRepository.statusTxt="Scanning Failed: ble not enabled"
            myRepository.isStatusChange = true
            return
        }
        //scan filter
        val filters: MutableList<ScanFilter> = ArrayList()
        val scanFilter: ScanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_STRING)))
            .build()
        filters.add(scanFilter)
        // scan settings
        // set low power scan mode
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        // start scan
        bleAdapter?.bluetoothLeScanner?.startScan(filters, settings, BLEScanCallback)
        //bleAdapter?.bluetoothLeScanner?.startScan(BLEScanCallback)

        myRepository.statusTxt = "Scanning...."
        myRepository.isStatusChange = true
        isScanning.set(true)

        Timer("SettingUp", false).schedule(3000) { stopScan() }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun stopScan(){
        bleAdapter?.bluetoothLeScanner?.stopScan(BLEScanCallback)
        isScanning.set(false)
        myRepository.statusTxt = "Scan finished. Click on the name to connect to the device."
        myRepository.isStatusChange = true

        scanResults = ArrayList() //list 초기화
        Log.d(TAG, "BLE Stop!")
    }

    /**
     * BLE Scan Callback
     */
    private val BLEScanCallback: ScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.i(TAG, "Remote device name: " + result.device.name)
            addScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                addScanResult(result)
            }
        }

        override fun onScanFailed(_error: Int) {
            Log.e(TAG, "BLE scan failed with code $_error")
            myRepository.statusTxt = "BLE scan failed with code $_error"
            myRepository.isStatusChange = true
        }

        /**
         * Add scan result
         */
        private fun addScanResult(result: ScanResult) {
            // get scanned device
            val device = result.device
            // get scanned device MAC address
            val deviceAddress = device.address
            val deviceName = device.name
            // add the device to the result list
            for (dev in scanResults!!) {
                if (dev.address == deviceAddress) return
            }
            scanResults?.add(result.device)
            // log
            myRepository.statusTxt = "add scanned device: $deviceAddress"
            myRepository.isStatusChange = true
            _listUpdate.postValue(Event(scanResults))
        }
    }


    fun onClickDisconnect(){
        myRepository.disconnectGattServer()
    }
    fun connectDevice(bluetoothDevice: BluetoothDevice){
        myRepository.connectDevice(bluetoothDevice)
    }

    fun registBroadCastReceiver(){
        myRepository.registerGattReceiver()
    }
    fun unregisterReceiver(){
        myRepository.unregisterReceiver()
    }

    fun onClickRead(){
        myRepository.readToggle()
    }

    fun onClickWrite(){

        val cmdBytes = ByteArray(2)
        cmdBytes[0] = 1
        cmdBytes[1] = 2

        myRepository.writeData(cmdBytes)

    }

}
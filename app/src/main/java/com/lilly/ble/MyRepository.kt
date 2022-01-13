package com.lilly.ble

import android.bluetooth.*
import android.content.*
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.lilly.ble.util.Event
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn


class MyRepository {

    private val TAG = "MyRepository"

    var statusTxt: String = ""

    var isConnected = MutableLiveData<Event<Boolean>>()

    var isRead = false
    var isStatusChange: Boolean = false


    var deviceToConnect: BluetoothDevice? = null
    var cmdByteArray: ByteArray? = null

    val readDataFlow = MutableLiveData<String>()
    val fetchStatusText = flow{
        while(true) {
            if(isStatusChange) {
                emit(statusTxt)
                isStatusChange = false
            }
        }
    }.flowOn(IO)







    /**
     * Handles various events fired by the Service.
     */
    private var mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG,"action ${intent.action}")
            when(intent.action){
                Actions.GATT_CONNECTED-> {
                    isConnected.postValue(Event(true))
                    intent.getStringExtra(Actions.MSG_DATA)?.let {
                        statusTxt = it
                        isStatusChange = true
                    }
                }
                Actions.GATT_DISCONNECTED->{
                    stopForegroundService()
                    isConnected.postValue(Event(false))
                    intent.getStringExtra(Actions.MSG_DATA)?.let{
                        statusTxt = it
                        isStatusChange = true
                    }
                }
                Actions.STATUS_MSG->{
                    intent.getStringExtra(Actions.MSG_DATA)?.let{
                        statusTxt = it
                        isStatusChange = true
                    }
                }
                Actions.READ_CHARACTERISTIC->{
                    intent.getByteArrayExtra(Actions.READ_BYTES)?.let{ bytes->
                        val hexString: String = bytes.joinToString(separator = " ") {
                            String.format("%02X", it)
                        }
                        readDataFlow.postValue(hexString)
                    }
                }

            }

        }
    }



    fun registerGattReceiver(){
        MyApplication.applicationContext().registerReceiver(mGattUpdateReceiver,
            makeGattUpdateIntentFilter())
    }
    fun unregisterReceiver(){
        MyApplication.applicationContext().unregisterReceiver(mGattUpdateReceiver)
    }
    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Actions.GATT_CONNECTED)
        intentFilter.addAction(Actions.GATT_DISCONNECTED)
        intentFilter.addAction(Actions.STATUS_MSG)
        intentFilter.addAction(Actions.READ_CHARACTERISTIC)
        return intentFilter
    }




    /**
     * Connect to the ble device
     */
    fun connectDevice(device: BluetoothDevice?) {
        deviceToConnect = device
        startForegroundService()
    }
    private fun startForegroundService(){
        Intent(MyApplication.applicationContext(), BleGattService::class.java).also { intent ->
            intent.action = Actions.START_FOREGROUND
            MyApplication.applicationContext().startForegroundService(intent)
        }
    }
    fun stopForegroundService(){
        Intent(MyApplication.applicationContext(), BleGattService::class.java).also { intent ->
            intent.action = Actions.STOP_FOREGROUND
            MyApplication.applicationContext().startForegroundService(intent)
        }
    }



    /**
     * Disconnect Gatt Server
     */
    fun disconnectGattServer() {
        Intent(MyApplication.applicationContext(), BleGattService::class.java).also { intent ->
            //MyApplication.applicationContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
            intent.action = Actions.DISCONNECT_DEVICE
            MyApplication.applicationContext().startForegroundService(intent)
        }
        deviceToConnect = null
    }

    fun writeData(byteArray: ByteArray){
        cmdByteArray = byteArray
        Intent(MyApplication.applicationContext(), BleGattService::class.java).also { intent ->
            //MyApplication.applicationContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
            intent.action = Actions.WRITE_DATA
            MyApplication.applicationContext().startForegroundService(intent)
        }
    }
    fun readToggle(){
        if(isRead){
            isRead = false
            stopNotification()
        }else{
            isRead = true
            startNotification()
        }
    }
    private fun startNotification(){
        Intent(MyApplication.applicationContext(), BleGattService::class.java).also { intent ->
            //MyApplication.applicationContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
            intent.action = Actions.START_NOTIFICATION
            MyApplication.applicationContext().startForegroundService(intent)
        }
    }
    private fun stopNotification(){
        Intent(MyApplication.applicationContext(), BleGattService::class.java).also { intent ->
            //MyApplication.applicationContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
            intent.action = Actions.STOP_NOTIFICATION
            MyApplication.applicationContext().startForegroundService(intent)
        }
    }

}
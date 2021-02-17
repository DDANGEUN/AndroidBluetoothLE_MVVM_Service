package com.lilly.ble

import android.bluetooth.*
import android.content.*
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lilly.ble.util.Event


class MyRepository {

    private val TAG = "MyRepository"

    val statusTxt = MutableLiveData<Event<String>>()
    val txtRead = MutableLiveData<Event<String>>()
    val isConnected = MutableLiveData<Event<Boolean>>()


    var mService: BleService? = null
    var mBound: Boolean? = null


    var deviceToConnect: BluetoothDevice? = null





    /**
     * Handles various events fired by the Service.
     */
    private var mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG,"action ${intent.action}")
            when(intent.action){
                ACTION_GATT_CONNECTED->{
                    isConnected.postValue(Event(true))
                    intent.getStringExtra(MSG_DATA)?.let{
                        statusTxt.postValue(Event(it))
                    }

                }
                ACTION_GATT_DISCONNECTED->{
                    isConnected.postValue(Event(false))
                    intent.getStringExtra(MSG_DATA)?.let{
                        statusTxt.postValue(Event(it))
                    }
                }
                ACTION_STATUS_MSG->{
                    intent.getStringExtra(MSG_DATA)?.let{
                        statusTxt.postValue(Event(it))
                    }
                }
                ACTION_READ_DATA->{
                    //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))

                    intent.getStringExtra(EXTRA_DATA)?.let{
                        txtRead.postValue(Event(it))
                    }
                }
            }

        }
    }

    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(TAG, "ServiceConnection: connected to service.")
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as BleService.LocalBinder
            mService = binder.service
            mBound = true
            mService?.connectDevice(deviceToConnect)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(TAG, "ServiceConnection: disconnected from service.")
            mBound = false
        }
    }

    fun registerGattReceiver(){
        MyApplication.applicationContext().registerReceiver(mGattUpdateReceiver,
            makeGattUpdateIntentFilter())
    }
    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_GATT_CONNECTED)
        intentFilter.addAction(ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(ACTION_READ_DATA)
        intentFilter.addAction(ACTION_STATUS_MSG)
        return intentFilter
    }




    /**
     * Connect to the ble device
     */
    fun connectDevice(device: BluetoothDevice?) {
        deviceToConnect = device
        // Bind to LocalService
        Intent(MyApplication.applicationContext(), BleService::class.java).also { intent ->
            MyApplication.applicationContext().bindService(
                intent,
                mServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }



    /**
     * Disconnect Gatt Server
     */
    fun disconnectGattServer() {
        MyApplication.applicationContext().unbindService(mServiceConnection)
        deviceToConnect = null
    }

    fun writeData(cmdByteArray: ByteArray){
        mService?.writeData(cmdByteArray)
    }

}
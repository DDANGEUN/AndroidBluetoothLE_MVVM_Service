package com.lilly.ble

import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.lilly.ble.util.BluetoothUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.*


class BleGattService : Service() {
    // Binder given to clients (notice class declaration below)
    private val mBinder: IBinder = LocalBinder()

    private val TAG = "BleService"

    private var fetchRead = false
    var isRead = false
    private var readTxt = "start"

    // ble Gatt
    private var bleGatt: BluetoothGatt? = null

    fun fetchReadData(): Flow<String> = flow {
        while (isRead) {
            if (fetchRead) {
                emit(readTxt)
                fetchRead = false
            }
        }

    }.flowOn(Dispatchers.IO)


    /**
     * Class used for the client Binder. The Binder object is responsible for returning an instance
     * of "MyService" to the client.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of MyService so clients can call public methods
        val service: BleGattService
            get() = this@BleGattService
    }

    /**
     * This is how the client gets the IBinder object from the service. It's retrieve by the "ServiceConnection"
     * which you'll see later.
     */
    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }


    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind called")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        super.onDestroy()
    }

    private fun broadcastUpdate(action: String, msg: String) {
        val intent = Intent(action)
        intent.putExtra(MSG_DATA, msg)
        sendBroadcast(intent)
    }


    /**
     * BLE gattClientCallback
     */
    private val gattClientCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)


            if (status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer("Bluetooth Gatt Fialure")
                return
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer("Disconnected")
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // update the connection status message
                broadcastUpdate(ACTION_GATT_CONNECTED, "Connected")
                Log.d(TAG, "Connected to the GATT server")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                broadcastUpdate(ACTION_GATT_DISCONNECTED, "Disconnected")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            // check if the discovery failed
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer("Device service discovery failed, status: $status")
                return
            }
            // log for successful discovery
            bleGatt = gatt
            Log.d(TAG, "Services discovery is successful")



        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            //Log.d(TAG, "characteristic changed: " + characteristic.uuid.toString())
            readCharacteristic(characteristic)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic written successfully")
            } else {
                disconnectGattServer("Characteristic write unsuccessful, status: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic read successfully")
                readCharacteristic(characteristic)
            } else {
                Log.e(TAG, "Characteristic read unsuccessful, status: $status")
                // Trying to read from the Time Characteristic? It doesnt have the property or permissions
                // set to allow this. Normally this would be an error and you would want to:
                // disconnectGattServer()
            }
        }

        /**
         * Log the value of the characteristic
         * @param characteristic
         */
        private fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {

            val msg = characteristic.getStringValue(0)
            readTxt = msg
            fetchRead = true
            Log.d(TAG, "read: $msg")
        }


    }

    /**
     * Connect to the ble device
     */
    fun connectDevice(device: BluetoothDevice?) {
        // update the status
        broadcastUpdate(ACTION_STATUS_MSG, "Connecting to ${device?.address}")
        bleGatt = device?.connectGatt(MyApplication.applicationContext(), false, gattClientCallback)
    }


    /**
     * Disconnect Gatt Server
     */
    fun disconnectGattServer(msg: String) {
        Log.d("hereigo", "Closing Gatt connection")
        // disconnect and close the gatt
        if (bleGatt != null) {
            bleGatt!!.disconnect()
            bleGatt!!.close()
        }
        broadcastUpdate(ACTION_GATT_DISCONNECTED, msg)
    }

    fun writeData(cmdByteArray: ByteArray) {
        val cmdCharacteristic = BluetoothUtils.findCommandCharacteristic(bleGatt!!)
        // disconnect if the characteristic is not found
        if (cmdCharacteristic == null) {
            disconnectGattServer("Unable to find cmd characteristic")
            return
        }

        cmdCharacteristic.value = cmdByteArray
        val success: Boolean = bleGatt!!.writeCharacteristic(cmdCharacteristic)
        // check the result
        if (!success) {
            Log.e(TAG, "Failed to write command")
        }
    }
    fun startRead(){
        isRead = true
        // find command characteristics from the GATT server
        val respCharacteristic = bleGatt?.let { BluetoothUtils.findResponseCharacteristic(it) }
        // disconnect if the characteristic is not found
        if (respCharacteristic == null) {
            disconnectGattServer("Unable to find characteristic")
            return
        }
        // READ
        bleGatt?.setCharacteristicNotification(respCharacteristic, true)
        // UUID for notification
        val descriptor: BluetoothGattDescriptor = respCharacteristic.getDescriptor(
            UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG)
        )
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        bleGatt?.writeDescriptor(descriptor)
    }

}
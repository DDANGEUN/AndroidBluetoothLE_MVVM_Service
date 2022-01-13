package com.lilly.ble

import android.app.*
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.lilly.ble.ui.main.MainActivity
import com.lilly.ble.util.BluetoothUtils
import org.koin.android.ext.android.inject
import java.util.*


class BleGattService : Service() {
    // Binder given to clients (notice class declaration below)
    private val mBinder: IBinder = LocalBinder()

    private val TAG = "BleService"


    private val myRepository: MyRepository by inject()

    // ble Gatt
    private var bleGatt: BluetoothGatt? = null


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Action Received = ${intent?.action}")
        // intent가 시스템에 의해 재생성되었을때 null값이므로 Java에서는 null check 필수
        when (intent?.action) {
            Actions.START_FOREGROUND -> {
                startForegroundService()
            }
            Actions.STOP_FOREGROUND -> {
                stopForegroundService()
            }
            Actions.DISCONNECT_DEVICE->{
                disconnectGattServer("Disconnected")
            }
            Actions.START_NOTIFICATION->{
                startNotification()
            }
            Actions.STOP_NOTIFICATION->{
                stopNotification()
            }
            Actions.WRITE_DATA->{
                myRepository.cmdByteArray?.let { writeData(it) }
            }

        }
        return START_STICKY
    }

    private fun startForegroundService() {
        startForeground()
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }



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


    private fun startForeground() {
        val channelId =
            createNotificationChannel()

        val notificationBuilder = NotificationCompat.Builder(this, channelId )
        val notificationIntent: Intent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Service is running in background")
            .setContentText("Tap to open")
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        //connect
        connectDevice(myRepository.deviceToConnect)

    }
    private fun createNotificationChannel(): String{
        val channelId = "my_service"
        val channelName = "My Background Service"
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_HIGH)
        chan.lightColor = Color.BLUE
        chan.importance = NotificationManager.IMPORTANCE_NONE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
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
        intent.putExtra(Actions.MSG_DATA, msg)
        sendBroadcast(intent)
    }
    private fun broadcastUpdate(action: String, readBytes: ByteArray) {
        val intent = Intent(action)
        intent.putExtra(Actions.READ_BYTES, readBytes)
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
                broadcastUpdate(Actions.GATT_CONNECTED, "Connected")
                Log.d(TAG, "Connected to the GATT server")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                broadcastUpdate(Actions.GATT_DISCONNECTED, "Disconnected")
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
            val msg = characteristic.value
            broadcastUpdate(Actions.READ_CHARACTERISTIC ,msg)
            Log.d(TAG, "read: $msg")
        }


    }

    /**
     * Connect to the ble device
     */
    private fun connectDevice(device: BluetoothDevice?) {
        // update the status
        broadcastUpdate(Actions.STATUS_MSG, "Connecting to ${device?.address}")
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
        broadcastUpdate(Actions.GATT_DISCONNECTED, msg)
    }

    private fun writeData(cmdByteArray: ByteArray) {
        val cmdCharacteristic = BluetoothUtils.findCommandCharacteristic(bleGatt!!)
        // disconnect if the characteristic is not found
        if (cmdCharacteristic == null) {
            myRepository.cmdByteArray = null
            disconnectGattServer("Unable to find cmd characteristic")
            return
        }

        cmdCharacteristic.value = cmdByteArray
        val success: Boolean = bleGatt!!.writeCharacteristic(cmdCharacteristic)
        // check the result
        if (!success) {
            Log.e(TAG, "Failed to write command")
        }
        myRepository.cmdByteArray = null

    }
    private fun startNotification(){
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
    private fun stopNotification(){
        // find command characteristics from the GATT server
        val respCharacteristic = bleGatt?.let { BluetoothUtils.findResponseCharacteristic(it) }
        // disconnect if the characteristic is not found
        if (respCharacteristic == null) {
            disconnectGattServer("Unable to find characteristic")
            return
        }
        bleGatt?.setCharacteristicNotification(respCharacteristic, false)
    }

}
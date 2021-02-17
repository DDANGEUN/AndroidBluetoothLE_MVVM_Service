package com.lilly.ble

import android.Manifest

// used to identify adding bluetooth names
const val REQUEST_ENABLE_BT = 1
// used to request fine location permission
const val REQUEST_ALL_PERMISSION = 2
val PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION
)

//사용자 BLE UUID Service/Rx/Tx
const val SERVICE_STRING = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
const val CHARACTERISTIC_COMMAND_STRING = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
const val CHARACTERISTIC_RESPONSE_STRING = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

//BluetoothGattDescriptor 고정
const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

const val STATE_DISCONNECTED = 0
const val STATE_CONNECTING = 1
const val STATE_CONNECTED = 2

const val ACTION_GATT_CONNECTED = "com.lilly.ble.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "com.lilly.ble.ACTION_GATT_DISCONNECTED"
const val ACTION_STATUS_MSG = "com.lilly.ble.ACTION_STATUS_MSG"
const val ACTION_READ_DATA= "com.lilly.ble.ACTION_READ_DATA"
const val EXTRA_DATA = "com.lilly.ble.EXTRA_DATA"
const val MSG_DATA = "com.lilly.ble.MSG_DATA"

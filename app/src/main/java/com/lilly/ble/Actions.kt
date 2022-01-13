package com.lilly.ble

object Actions {
    private const val prefix = "lilly.ble.mvvmservice"
    const val START_FOREGROUND = prefix + "startforeground"
    const val STOP_FOREGROUND = prefix + "stopforeground"
    const val DISCONNECT_DEVICE = prefix + "disconnectdevice"
    const val CONNECT_DEVICE = prefix + "disconnectdevice"
    const val START_NOTIFICATION = prefix + "startnotification"
    const val STOP_NOTIFICATION = prefix + "stopnotification"
    const val WRITE_DATA = prefix + "writedata"
    const val READ_CHARACTERISTIC= prefix + "readcharacteristic"
    const val READ_BYTES = prefix + "readbytes"
    const val GATT_CONNECTED = prefix + "gattconnected"
    const val GATT_DISCONNECTED = prefix + "gattdisconnected"
    const val STATUS_MSG = prefix + "statusmsg"
    const val MSG_DATA = prefix + "msgdata"
}
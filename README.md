# AndroidBluetoothLE_MVVM_Service

bluetooth low energy example (mvvm) + (Foreground)Service

　  



## Info

- *This code is Bluetooth LE MVVM code using Service.*  
If you want to see the Ble implemented in Repository without using the service,  
See [**AndroidBluetoothLE_MVVM**](https://github.com/DDANGEUN/AndroidBluetoothLE_MVVM)  
　  
- If you want to see your ble device like this code preview, ***modify UUID*** in **Constants.kt**  
```Kotlin
//사용자 BLE UUID Service/Rx/Tx
const val SERVICE_STRING = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
const val CHARACTERISTIC_COMMAND_STRING = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
const val CHARACTERISTIC_RESPONSE_STRING = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
```
　  



## Profile
<img src = "https://github.com/DDANGEUN/AndroidBluetoothLE_MVVM_Service/blob/main/structimg.png">  



　  



## Preview
<img src = "https://github.com/DDANGEUN/AndroidBluetoothLE_MVVM_Service/blob/main/ble.gif" width="30%">


　  


## Blog
- BLE Example : https://ddangeun.tistory.com/98
- Using Service in MVVM : https://ddangeun.tistory.com/124

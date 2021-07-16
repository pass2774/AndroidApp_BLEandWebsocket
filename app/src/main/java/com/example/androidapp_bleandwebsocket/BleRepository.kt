package com.example.androidapp_bleandwebsocket

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context.BLUETOOTH_SERVICE
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.example.androidapp_bleandwebsocket.util.BluetoothUtils
import com.example.androidapp_bleandwebsocket.util.Event
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.*
import kotlin.concurrent.schedule


//For Websocket
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import okio.internal.commonToUtf8String
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
//Not used, but going to be needed for security
import javax.net.ssl.SSLSocketFactory

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BleRepository {

    private val TAG = "Central"

    // ble manager
    val bleManager: BluetoothManager =
        MyApplication.applicationContext().getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    // ble adapter
    val bleAdapter: BluetoothAdapter?
        get() = bleManager.adapter
    // ble Gatt
    private var bleGatt: BluetoothGatt? = null

    // scan results
    var scanResults: ArrayList<BluetoothDevice>? = ArrayList()

    var statusTxt: String = ""
    var txtRead: String = ""
    lateinit var sensorRead: Collection<UShort>

    var isStatusChange: Boolean = false
    var isTxtRead: Boolean = false
    fun fetchReadText() = flow{
        while(true) {
            if(isTxtRead) {
                emit(txtRead)
//                emit(sensorRead.contentToString())
                isTxtRead = false
            }
        }
    }.flowOn(Dispatchers.Default)
    fun fetchReadSensor() = flow{
        while(true) {
            if(isTxtRead) {
//                emit(txtRead)
                emit(sensorRead)
                isTxtRead = false
            }
        }
    }.flowOn(Dispatchers.Default)


    fun fetchStatusText() = flow{
        while(true) {
            if(isStatusChange) {
                emit(statusTxt)
                isStatusChange = false
            }
        }
    }.flowOn(Dispatchers.Default)


    val requestEnableBLE = MutableLiveData<Event<Boolean>>()
    val isScanning = MutableLiveData(Event(false))
    val isConnect = MutableLiveData(Event(false))
    val listUpdate = MutableLiveData<Event<ArrayList<BluetoothDevice>?>>()
    val scrollDown = MutableLiveData<Event<Boolean>>()


    fun startScan() {
        // check ble adapter and ble enabled
        if (bleAdapter == null || !bleAdapter?.isEnabled!!) {
            requestEnableBLE.postValue(Event(true))
            statusTxt ="Scanning Failed: ble not enabled"
            isStatusChange = true
            return
        }
        //scan filter
        val filters: MutableList<ScanFilter> = ArrayList()
//        val scanFilter: ScanFilter = ScanFilter.Builder()
//            .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_STRING)))
//            .build()
        val scanFilter: ScanFilter = ScanFilter.Builder()
            .setDeviceName("Project Zero R2")
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

        statusTxt = "Scanning...."
        isStatusChange = true
        isScanning.postValue(Event(true))

        Timer("SettingUp", false).schedule(3000) { stopScan() }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun stopScan(){
        bleAdapter?.bluetoothLeScanner?.stopScan(BLEScanCallback)
        isScanning.postValue(Event(false))
        statusTxt = "Scan finished. Click on the name to connect to the device."
        isStatusChange = true


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
            statusTxt = "BLE scan failed with code $_error"
            isStatusChange = true
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
            statusTxt = "add scanned device: $deviceAddress"
            isStatusChange = true
            listUpdate.postValue(Event(scanResults))
        }
    }

    /**
     * BLE gattClientCallback
     */
    private val gattClientCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if( status == BluetoothGatt.GATT_FAILURE ) {
                disconnectGattServer()
                return
            } else if( status != BluetoothGatt.GATT_SUCCESS ) {
                disconnectGattServer()
                return
            }
            if( newState == BluetoothProfile.STATE_CONNECTED ) {
                // update the connection status message

                statusTxt = "Connected"
                isStatusChange = true
                Log.d(TAG, "Connected to the GATT server")
                gatt.discoverServices()
            } else if ( newState == BluetoothProfile.STATE_DISCONNECTED ) {
                disconnectGattServer()
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            // check if the discovery failed
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Device service discovery failed, status: $status")
                return
            }
            // log for successful discovery
            Log.d(TAG, "Services discovery is successful")
            isConnect.postValue(Event(true))
            // find command characteristics from the GATT server
            val respCharacteristic = gatt?.let { BluetoothUtils.findResponseCharacteristic(it) }
            // disconnect if the characteristic is not found
            if( respCharacteristic == null ) {
                Log.e(TAG, "Unable to find cmd characteristic")
                disconnectGattServer()
                return
            }
            gatt.setCharacteristicNotification(respCharacteristic, true)
            // UUID for notification
            val descriptor: BluetoothGattDescriptor = respCharacteristic.getDescriptor(
                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG)
            )
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
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
                Log.e(TAG, "Characteristic write unsuccessful, status: $status")
                disconnectGattServer()
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
            var msg = characteristic.value
//            txtRead = msg.toString()+"\n" //Original code
//            txtRead =msg.contentToString()

            val shorts = msg.asList().chunked(2).map{ (l, h) -> (l.toUInt() + h.toUInt().shl(8)).toUShort() }.toUShortArray()
            sensorRead=shorts
            txtRead=shorts.contentToString()
            val dateAndtime: LocalDateTime = LocalDateTime.now()
//            val TimeStampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS")

            try{
                if(webSocketClient?.isOpen() == true) {
                    webSocketClient?.send(
                        //data frame
                        //my pick: id, message_type,time, sensor1/datainfo(ex: uint16_t:sampling rate=50Hz)/data(0:1:2:3:...:N),sensor2/datatype(ex: double)/data(0:1:2:3:...:M),
                        //"android_test,(message_type),${dateAndtime.format(TimeStampFormatter)},ECG/uint16_t/20ms/${shorts[0]}:${shorts[1]}:${shorts[2]}:${shorts[3]}:${shorts[4]},Temperature/int16_t/1000ms/${shorts[0]},BldPrs/uint16_t/1000ms/${shorts[0]}"
                        "android_test,(message_type),${dateAndtime},ECG/uint16_t/20/${shorts[0]}:${shorts[1]}:${shorts[2]}:${shorts[3]}:${shorts[4]},Temperature/int16_t/1000/${shorts[0]},BldPrs/uint16_t/1000/${shorts[0]}"
                    )
                }
            } catch(e:NumberFormatException){
                return
            }
            Log.d(TAG, "read: "+txtRead)
            isTxtRead = true
        }


    }

    /**
     * Connect to the ble device
     */
    fun connectDevice(device: BluetoothDevice?) {
        // update the status
        statusTxt = "Connecting to ${device?.address}"
        isStatusChange = true
        bleGatt = device?.connectGatt(MyApplication.applicationContext(), false, gattClientCallback)

        initWebSocket_LiveDB() // websocket - Joonhwa Choi
    }



    /**
     * Disconnect Gatt Server
     */
    fun disconnectGattServer() {
        Log.d(TAG, "Closing Gatt connection")
        // disconnect and close the gatt
        if (bleGatt != null) {
            bleGatt!!.disconnect()
            bleGatt!!.close()
            statusTxt = "Disconnected"
            isStatusChange = true
            isConnect.postValue(Event(false))
        }
        webSocketClient?.close()

    }

    fun writeData(cmdByteArray: ByteArray){
        val cmdCharacteristic = BluetoothUtils.findCommandCharacteristic(bleGatt!!)
        // disconnect if the characteristic is not found
        if (cmdCharacteristic == null) {
            Log.e(TAG, "Unable to find cmd characteristic")
            disconnectGattServer()
            return
        }

        cmdCharacteristic.value = cmdByteArray
        val success: Boolean = bleGatt!!.writeCharacteristic(cmdCharacteristic)
        // check the result
        if( !success ) {
            Log.e(TAG, "Failed to write command")
        }
    }

    ///////////////////Websocket part - Added by Joonhwa Choi ////////////////////
    //For Websocket
    // 전역 변수로 바인딩 객체 선언
    // 매번 null 체크를 할 필요 없이 편의성을 위해 바인딩 변수 재 선언
//    private val binding get() = mBinding!!
//    private lateinit var webSocketClient: WebSocketClient
    private var webSocketClient: WebSocketClient? = null

    companion object {
        const val WEBSOCKET_ECHO_URL = "wss://echo.websocket.org"
        const val WEBSOCKET_LIVEDB_JOONHWA_URL = "ws://147.46.241.33:6716/"   // Raspberry WS server
        const val TAG = "Websocket"
    }

    private fun initWebSocket_LiveDB() {
        val URI_LiveDB: URI? = URI(WEBSOCKET_LIVEDB_JOONHWA_URL)
        Log.d(TAG, "initWebSocket_LiveDB")
        try{
            createWebSocketClient(URI_LiveDB)
            webSocketClient?.connect()
        } catch(e:NumberFormatException){
            return
        }
    }

    private fun createWebSocketClient(coinbaseUri: URI?) {
        webSocketClient = object : WebSocketClient(coinbaseUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d(TAG, "onOpen")
//                subscribe()   //for bitcoin
//                webSocketClient.send(
//                    //data frame
//                    //my pick: dev1, time, sensor1/datainfo(ex: uint16_t:sampling rate=50Hz)/data(0:1:2:3:...:N),sensor2/datatype(ex: double)/data(0:1:2:3:...:M),
//                    "monitor_MFC1,0,1,2,3,4"
//                )
            }

            override fun onMessage(message: String?) {
                Log.d(TAG, "onMessage: $message")
                updateTextView(message)
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(TAG, "onClose")
            }

            override fun onError(ex: Exception?) {
                Log.e(TAG, "onError: ${ex?.message}")
            }
            private fun updateTextView(message: String?) {
                message?.let {
                    val moshi = Moshi.Builder().build()
                    val adapter: JsonAdapter<WebsocketDataAdapter> = moshi.adapter(WebsocketDataAdapter::class.java)
//                    val bitcoin = adapter.fromJson(message)
//                    runOnUiThread { binding.btcPriceTv.text = message }
                }
            }
        }
    }


}


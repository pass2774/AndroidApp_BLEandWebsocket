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
import com.example.androidapp_bleandwebsocket.BleConstants.*



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
import com.example.androidapp_bleandwebsocket.CsvHelperSAF


import android.os.Handler
import android.os.Looper


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
    var txtRead2: String = ""
    var txtRead3: String = ""
    lateinit var sensorRead: Collection<UShort>
    lateinit var sensor2Read: Collection<Int>
    lateinit var sensor3Read: Collection<Int>

    var isStatusChange: Boolean = false
    var isTxtRead: Boolean = false
    var isTxtRead2: Boolean = false
    var isTxtRead3: Boolean = false
    var isSensorRead: Boolean = false
    var isSensor2Read: Boolean = false
    var isSensor3Read: Boolean = false

    var csvHelperSAF:CsvHelperSAF? = null

    fun fetchReadText() = flow{
        while(true) {
            if(isTxtRead) {
                emit(txtRead)
//                emit(sensorRead.contentToString())
                isTxtRead = false
            }
        }
    }.flowOn(Dispatchers.Default)
    fun fetchReadText2() = flow{
        while(true) {
            if(isTxtRead2) {
                emit(txtRead2)
                isTxtRead2 = false
            }
        }
    }.flowOn(Dispatchers.Default)
    fun fetchReadText3() = flow{
        while(true) {
            if(isTxtRead3) {
                emit(txtRead3)
                isTxtRead3 = false
            }
        }
    }.flowOn(Dispatchers.Default)
    fun fetchReadSensor() = flow{
        while(true) {
            if(isSensorRead) {
//                emit(txtRead)
                emit(sensorRead)
                isSensorRead = false
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
            val respCharacteristic = gatt?.let { BluetoothUtils.findResponseCharacteristic(it,CHARACTERISTIC_READ_SENSOR1_STRING) }
            // disconnect if the characteristic is not found
            if( respCharacteristic == null ) {
                Log.e(TAG, "Unable to find cmd characteristic")
                disconnectGattServer()
                return
            }
            gatt.setCharacteristicNotification(respCharacteristic, true)

            val respCharacteristic2 = gatt?.let { BluetoothUtils.findResponseCharacteristic(it,CHARACTERISTIC_READ_SENSOR2_STRING) }
            // disconnect if the characteristic is not found
            if( respCharacteristic2 == null ) {
                Log.e(TAG, "Unable to find cmd characteristic")
                disconnectGattServer()
                return
            }
            gatt.setCharacteristicNotification(respCharacteristic2, true)


            // UUID for notification
            val descriptor: BluetoothGattDescriptor = respCharacteristic.getDescriptor(
                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG)
            )
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)

            // UUID for notification
            val descriptor2: BluetoothGattDescriptor = respCharacteristic2.getDescriptor(
                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG)
            )

            descriptor2.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            Log.e(TAG, "Go!!")
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                if(gatt.writeDescriptor(descriptor2)){
                    Log.e(TAG, "Success ...writeDescriptor")
                }else{
                    Log.e(TAG, "Fail ...writeDescriptor")
                }
            }, 1000)



        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d(TAG, "characteristic changed: " + characteristic.uuid.toString())
            when(characteristic.uuid.toString()){
                CHARACTERISTIC_READ_SENSOR1_STRING -> readSensor1Characteristic(characteristic)
                CHARACTERISTIC_READ_SENSOR2_STRING -> readSensor2Characteristic(characteristic)
            }
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
                readSensor1Characteristic(characteristic)
                readSensor2Characteristic(characteristic)
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

        private fun readSensor1Characteristic(characteristic: BluetoothGattCharacteristic) {
            var msg = characteristic.value
//            txtRead = msg.toString()+"\n" //Original code
//            txtRead =msg.contentToString()

            val shorts = msg.asList().chunked(2).map{ (l, h) -> (l.toUInt() + h.toUInt().shl(8)).toUShort() }.toUShortArray()
            sensorRead=shorts
//            txtRead=shorts.contentToString() // --> [a, b, c]
//            txtRead=shorts.JoinToString(prefix="<",separator = "|",postfix=">") //--> <a|b|c>
            txtRead=shorts.joinToString(prefix="",separator = ",",postfix="")
            txtRead="Sensor1:"+txtRead
            csvHelperSAF?.let{
                it.writeSensorDataToCsv(txtRead)
            }

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
            isSensorRead = true
        }
        private fun readSensor2Characteristic(characteristic: BluetoothGattCharacteristic) {
            var msg = characteristic.value.toList()
            val ints_imu = msg.subList(0,8).chunked(2).map{ (l, h) -> (l.toUInt().shl(8) + h.toUInt()).toShort().toInt() }.t11oIntArray()
            val ints_barro = msg.subList(8,20).chunked(4).map{ (byte0,byte1,byte2,byte3) -> (byte0.toUInt().shl(24)+byte1.toUInt().shl(16)+byte2.toUInt().shl(8)+byte3.toUInt()).toInt() }.toIntArray()
//            val ints = msg.asList().chunked(4).map{ (byte0,byte1,byte2,byte3) -> (byte0.toUInt().shl(24)+byte1.toUInt().shl(16)+byte2.toUInt().shl(8)+byte3.toUInt()) }.toUIntArray()
//            sensor2Read=ints
//            sensor2Read=ints_imu.toList().toCollection((ArrayList()))
//            sensor2Read=ints_imu.toList()
            sensor2Read=ints_imu.toList().toCollection((ArrayList()))
            sensor3Read=ints_barro.toList().toCollection((ArrayList()))
            Log.d(TAG, "read!!: "+txtRead2.toString())
            txtRead2=sensor2Read.joinToString(prefix="",separator = ",",postfix="")
            txtRead2="Sensor2:"+txtRead2
            txtRead3=sensor3Read.joinToString(prefix="",separator = ",",postfix="")
            txtRead3="Sensor3:"+txtRead3
            csvHelperSAF?.let{
                it.writeSensorDataToCsv(txtRead2)
                it.writeSensorDataToCsv(txtRead3)
            }

            val dateAndtime: LocalDateTime = LocalDateTime.now()
//            val TimeStampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS")

//            try{
//                if(webSocketClient?.isOpen() == true) {
//                    webSocketClient?.send(
//                        //data frame
//                        //my pick: id, message_type,time, sensor1/datainfo(ex: uint16_t:sampling rate=50Hz)/data(0:1:2:3:...:N),sensor2/datatype(ex: double)/data(0:1:2:3:...:M),
//                        //"android_test,(message_type),${dateAndtime.format(TimeStampFormatter)},ECG/uint16_t/20ms/${shorts[0]}:${shorts[1]}:${shorts[2]}:${shorts[3]}:${shorts[4]},Temperature/int16_t/1000ms/${shorts[0]},BldPrs/uint16_t/1000ms/${shorts[0]}"
//                        "android_test,(message_type),${dateAndtime},ECG/uint16_t/20/${shorts[0]}:${shorts[1]}:${shorts[2]}:${shorts[3]}:${shorts[4]},Temperature/int16_t/1000/${shorts[0]},BldPrs/uint16_t/1000/${shorts[0]}"
//                    )
//                }
//            } catch(e:NumberFormatException){
//                return
//            }
//            Log.d(TAG, "read: "+txtRead2)
            isTxtRead2 = true
            isTxtRead3 = true
            isSensor2Read = true
            isSensor3Read = true
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


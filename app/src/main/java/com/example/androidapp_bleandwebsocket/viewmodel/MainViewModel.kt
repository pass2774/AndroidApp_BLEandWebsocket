package com.example.androidapp_bleandwebsocket.viewmodel

import android.bluetooth.*
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import com.example.androidapp_bleandwebsocket.*
import com.example.androidapp_bleandwebsocket.util.Event
import java.util.*

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.annotation.MainThread
import com.example.androidapp_bleandwebsocket.CsvHelperSAF

class MainViewModel(private val bleRepository: BleRepository) : ViewModel() {
    val statusTxt: LiveData<String>
        get() = bleRepository.fetchStatusText().asLiveData(viewModelScope.coroutineContext)
    val readTxt: LiveData<String>
        get() = bleRepository.fetchReadText().asLiveData(viewModelScope.coroutineContext)
    //Joonhwa - new livedata
    val readSensor: LiveData<Collection<UShort>>
        get() = bleRepository.fetchReadSensor().asLiveData(viewModelScope.coroutineContext)

    //Joonhwa - new activty
    private val _openEvent = MutableLiveData<Event<String>>()
    val openEvent: LiveData<Event<String>> get() = _openEvent


    //ble adapter
    private val bleAdapter: BluetoothAdapter?
        get() = bleRepository.bleAdapter


    val requestEnableBLE : LiveData<Event<Boolean>>
        get() = bleRepository.requestEnableBLE
    val listUpdate : LiveData<Event<ArrayList<BluetoothDevice>?>>
        get() = bleRepository.listUpdate

    val _isScanning: LiveData<Event<Boolean>>
        get() = bleRepository.isScanning
    var isScanning = ObservableBoolean(false)
    val _isConnect: LiveData<Event<Boolean>>
        get() = bleRepository.isConnect
    var isConnect = ObservableBoolean(false)



    /**
     *  Start BLE Scan
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun onClickScan(){
        bleRepository.startScan()
    }
    fun onClickDisconnect(){
        bleRepository.disconnectGattServer()
    }
    fun connectDevice(bluetoothDevice: BluetoothDevice){
        bleRepository.connectDevice(bluetoothDevice)
    }

    // Added by Joonhwa Choi for service binding, from https://github.com/uberchilly/BoundServiceMVVM
    fun onStart() {
//        bleRepository.addConnectionListener(this)
    }

    fun onStop() {
//        bleRepository.removeConnectionListener(this)
    }

    fun transferUriInfo(csvHelperSAF:CsvHelperSAF){
        bleRepository.csvHelperSAF = csvHelperSAF
    }


    fun onClickWrite(){

        val cmdBytes = ByteArray(1)
        cmdBytes[0] = 1
//        cmdBytes[1] = 2

        bleRepository.writeData(cmdBytes)
    }

    fun onClickGotoGraph(){
//        var intent = Intent(this, ChartActivity::class.java)
//        startActivity(intent)
        val text: String = "sibal"
        _openEvent.value = Event(text)
    }
}


inline fun <T> LiveData<Event<T>>.eventObserve(
    owner: LifecycleOwner,
    crossinline onChanged: (T) -> Unit
): Observer<Event<T>> {
    val wrappedObserver = Observer<Event<T>> { t ->
        t.getContentIfNotHandled()?.let {
            onChanged.invoke(it)
        }
    }
    observe(owner, wrappedObserver)
    return wrappedObserver
}
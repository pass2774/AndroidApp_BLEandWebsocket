package com.example.androidapp_bleandwebsocket.main


//For debugging
import android.util.Log

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidapp_bleandwebsocket.PERMISSIONS
import com.example.androidapp_bleandwebsocket.R
import com.example.androidapp_bleandwebsocket.REQUEST_ALL_PERMISSION
import com.example.androidapp_bleandwebsocket.adapter.BleListAdapter
import com.example.androidapp_bleandwebsocket.databinding.ActivityMainBinding
import com.example.androidapp_bleandwebsocket.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.example.androidapp_bleandwebsocket.util.Event
import com.example.androidapp_bleandwebsocket.FragmentChart
import com.example.androidapp_bleandwebsocket.CsvHelperSAF

import android.net.Uri
import java.io.FileOutputStream
import kotlinx.coroutines.*
////For Websocket
//import com.squareup.moshi.JsonAdapter
//import com.squareup.moshi.Moshi
//import org.java_websocket.client.WebSocketClient
//import org.java_websocket.handshake.ServerHandshake
//import java.lang.Exception
//import java.net.URI
////Not used, but going to be needed for security
//import javax.net.ssl.SSLSocketFactory

class MainActivity : AppCompatActivity() {
//    출처: https://namget.tistory.com/entry/코틀린-mvvm패턴-속-application-context-가져오기 [남갯,YTS의 개발,일상블로그]
    lateinit var context: Context
    init{
        instance = this
    }
    companion object {
        private var instance: MainActivity? = null
        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

    private val viewModel by viewModel<MainViewModel>()
    private var adapter: BleListAdapter? = null
    val fragmentChart = FragmentChart()

    //테스트용 파일명1 & 2
    val WRITE_REQUEST_CODE: Int = 43
    lateinit var Uri_CsvFile: Uri
    lateinit var csvHelperSAF: CsvHelperSAF


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(
            this,
            R.layout.activity_main
        )
        binding.viewModel = viewModel

        binding.rvBleList.setHasFixedSize(true)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvBleList.layoutManager = layoutManager


        adapter = BleListAdapter()
        binding.rvBleList.adapter = adapter
        adapter?.setItemClickListener(object : BleListAdapter.ItemClickListener {
            override fun onClick(view: View, device: BluetoothDevice?) {
                if (device != null) {
                    viewModel.connectDevice(device)
                }
            }
        })

        // check if location permission
        if (!hasPermissions(this, PERMISSIONS)) {
            requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION)
        }
        initObserver(binding)


        val bundle = Bundle()
        bundle.putString("Key", "Hello FragmentA")
        fragmentChart.arguments = bundle
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.frameLayout, fragmentChart)
        transaction.commit()


        val fileName = "CsvTest.csv"   // 1
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {  // 2
            addCategory(Intent.CATEGORY_OPENABLE)   // 3
            type = "text/csv"    // 4
            putExtra(Intent.EXTRA_TITLE, fileName)   // 5
        }
        startActivityForResult(intent, WRITE_REQUEST_CODE)    // 6
    }


    private fun initObserver(binding: ActivityMainBinding){
        viewModel.requestEnableBLE.observe(this, {
            it.getContentIfNotHandled()?.let {
                requestEnableBLE()
            }
        })
        viewModel.listUpdate.observe(this, {
            it.getContentIfNotHandled()?.let { scanResults ->
                adapter?.setItem(scanResults)
            }
        })


        viewModel._isScanning.observe(this,{
            it.getContentIfNotHandled()?.let{ scanning->
                viewModel.isScanning.set(scanning)
            }
        })
        viewModel._isConnect.observe(this,{
            it.getContentIfNotHandled()?.let{ connect->
                viewModel.isConnect.set(connect)
            }
        })
        viewModel.statusTxt.observe(this,{

           binding.statusText.text = it

        })

        viewModel.readTxt.observe(this,{

//            binding.txtRead.append(it)
            binding.txtRead.setText(it)
//            writeSensorDataToCsv(Uri_CsvFile, it)
            csvHelperSAF.writeSensorDataToCsv(it)
            if ((binding.txtRead.measuredHeight - binding.scroller.scrollY) <=
                (binding.scroller.height + binding.txtRead.lineHeight)) {
                binding.scroller.post {
                    binding.scroller.smoothScrollTo(0, binding.txtRead.bottom)
                }
            }

        })

        viewModel.readSensor.observe(this,{
//        출처: https://juahnpop.tistory.com/225 [Blacklog] - communication btw activity and fragment
            fragmentChart.updateChartData(it)
        })

        //Opening new activity
        viewModel.openEvent.observe(this,{
            it.getContentIfNotHandled()?.let{ connect->
                var intent = Intent(this, ChartActivity::class.java)
//            intent.putExtra("sample", sampleText)
                startActivity(intent)
            }
        })
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                Log.i("SAF", "Uri: $uri")
//                Uri_CsvFile=uri
                //writeCsv(uri)
                csvHelperSAF = CsvHelperSAF(uri)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // finish app if the BLE is not supported
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() { // onDestroy 에서 binding class 인스턴스 참조를 정리해주어야 한다.
        super.onDestroy()
    }


    private val requestEnableBleResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            // do somthing after enableBleRequest
        }
    }

    /**
     * Request BLE enable
     */
    private fun requestEnableBLE() {
        val bleEnableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        requestEnableBleResult.launch(bleEnableIntent)
    }

    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }
    // Permission check
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ALL_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
                } else {
                    requestPermissions(permissions, REQUEST_ALL_PERMISSION)
                    Toast.makeText(this, "Permissions must be granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
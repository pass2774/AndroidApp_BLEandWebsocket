package com.example.androidapp_bleandwebsocket

import android.content.Context
import java.util.*
import android.net.Uri
import java.io.FileOutputStream
import kotlinx.coroutines.*
import com.example.androidapp_bleandwebsocket.main.MainActivity
//Original Reference:
// https://codechacha.com/ko/android-storage-access-framework/
//Modified for CSV file R/W by Joonhwa Choi
class CsvHelperSAF(private val fileUri: Uri) {
    var context:Context = MainActivity.applicationContext()

    fun writeSensorDataToCsv(sensor_data:String) {
        GlobalScope.launch {
            //String: The string representation of the file mode. Can be "r", "w", "wt", "wa", "rw" or "rwt". SeeParcelFileDescriptor#parseMode for more details. This value cannot be null.
            context.contentResolver.openFileDescriptor(fileUri, "wa").use {    // wa: write & append
//                MainActivity.applicationContext().contentResolver.openFileDescriptor(fileUri, "wa").use {    // wa: write & append
                FileOutputStream(it!!.fileDescriptor).use { it ->
                    writeSensorData(it,sensor_data)
                    it.close()
                }
            }
            withContext(Dispatchers.Main) {
//                Toast.makeText(applicationContext, "Logging CSV data", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun writeSensorData(outStream: FileOutputStream,input_data:String) {
        outStream.write(input_data.plus("\n").toByteArray())
    }


//    private fun writeCsv() {
//        GlobalScope.launch {
//            Context.contentResolver.openFileDescriptor(fileUri, "w").use {    // 1
//                FileOutputStream(it!!.fileDescriptor).use { it ->   // 2
//                    writeFromRawDataToFile(it)    // 3
//                    it.close()
//                }
//            }
//            withContext(Dispatchers.Main) {   //  4
//                Toast.makeText(applicationContext, "Done writing an image", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun writeFromRawDataToFile(outStream: FileOutputStream) {
//        val data0 = "Csv testing,Hello,my buddy"
//        outStream.write(data0.toByteArray())   // 6
//    }

}
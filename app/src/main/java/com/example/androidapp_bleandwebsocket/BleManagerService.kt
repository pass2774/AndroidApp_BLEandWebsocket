package com.example.androidapp_bleandwebsocket

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import org.koin.android.ext.android.inject
import android.util.Log

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat


//Original Reference - https://jizard.tistory.com/217
class BleManagerService : Service() {
    private val binder = LocalBinder()
    // lazy inject BusinessService into property
    private val bleManager: BleRepository by inject()
    val CHANNEL_ID = "foreground_service_channel" // 임의의 채널 ID
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        Log.e(TAG, "Action Received = ${intent?.action}")
//    // intent가 시스템에 의해 재생성되었을때 null값이므로 Java에서는 null check 필수
//        when (intent?.action) {
//            BleManagerAction.START_FOREGROUND -> {
//                Log.e(TAG, "Start Foreground 인텐트를 받음")
//                startForegroundService()
//            }
//            BleManagerAction.STOP_FOREGROUND -> {
//                Log.e(TAG, "Stop Foreground 인텐트를 받음")
//                stopForegroundService()
//            }
//            BleManagerAction.PREV -> Log.e(TAG, "Clicked = 이전")
//            BleManagerAction.PLAY -> Log.e(TAG, "Clicked = 재생")
//            BleManagerAction.NEXT -> Log.e(TAG, "Clicked = 다음")
//        }
//        return START_STICKY
//    }

    private fun startForegroundService() {
        val notification = BleManagerNofitication.createNotification(this)
        startForeground(NOTIFICATION_ID, notification)
    }

//    private fun stopForegroundService() {
//        stopForeground(true)
//        stopSelf()
//    }
    private fun startNotification() {
//Notification example 1
        val notification = BleManagerNofitication.createNotification(this)
        startForeground(NOTIFICATION_ID, notification)
// Notification example 2
//        https://proandroiddev.com/bound-and-foreground-services-in-android-a-step-by-step-guide-5f8362f4ae20
//        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel(
//                CHANNEL_ID,
//                "My Channel",
//                NotificationManager.IMPORTANCE_DEFAULT
//            )
//        } else {
//            TODO("VERSION.SDK_INT < O")
//        }
//        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
//        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("A service is running in the background")
//            .setContentText("Generating random number").build()
//        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onCreate() {
        super.onCreate()
        startNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): BleManagerService = this@BleManagerService
    }

    companion object{
        const val TAG = "[BleManangerService]"
        const val NOTIFICATION_ID = 20
    }


}
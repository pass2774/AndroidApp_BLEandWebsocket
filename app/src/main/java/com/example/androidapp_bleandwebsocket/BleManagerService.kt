package com.example.androidapp_bleandwebsocket

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
//Original Reference - https://jizard.tistory.com/217
class BleManagerService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "Action Received = ${intent?.action}")
    // intent가 시스템에 의해 재생성되었을때 null값이므로 Java에서는 null check 필수
        when (intent?.action) {
            BleManagerAction.START_FOREGROUND -> {
                Log.e(TAG, "Start Foreground 인텐트를 받음")
                startForegroundService()
            }
            BleManagerAction.STOP_FOREGROUND -> {
                Log.e(TAG, "Stop Foreground 인텐트를 받음")
                stopForegroundService()
            }
            BleManagerAction.PREV -> Log.e(TAG, "Clicked = 이전")
            BleManagerAction.PLAY -> Log.e(TAG, "Clicked = 재생")
            BleManagerAction.NEXT -> Log.e(TAG, "Clicked = 다음")
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = BleManagerNofitication.createNotification(this)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object{
        const val TAG = "[BleManangerService]"
        const val NOTIFICATION_ID = 20
    }


}
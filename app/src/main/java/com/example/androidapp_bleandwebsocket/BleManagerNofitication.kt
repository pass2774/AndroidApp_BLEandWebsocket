package com.example.androidapp_bleandwebsocket

import android.app.*
import com.example.androidapp_bleandwebsocket.main.MainActivity
import android.content.Context
import android.content.Intent

import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.core.app.NotificationCompat

//    출처: https://jizard.tistory.com/217 [Lou]
object BleManagerNofitication {
    const val CHANNEL_ID = "foreground_service_channel" // 임의의 채널 ID
    fun createNotification( context: Context ): Notification {
    // 알림 클릭시 MainActivity로 이동됨
        val notificationIntent = Intent(context, MainActivity::class.java)
            notificationIntent.action = BleManagerAction.MAIN
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent
            .getActivity(context, 0, notificationIntent, FLAG_UPDATE_CURRENT)
        //각 버튼들에 관한 Intent
        val prevIntent = Intent(context, BleManagerService::class.java)
            prevIntent.action = BleManagerAction.PREV
        val prevPendingIntent = PendingIntent
            .getService(context, 0, prevIntent, 0)
        val playIntent = Intent(context, BleManagerService::class.java)
        playIntent.action = BleManagerAction.PLAY
        val playPendingIntent = PendingIntent
            .getService(context, 0, playIntent, 0)
        val nextIntent = Intent(context, BleManagerService::class.java)
        nextIntent.action = BleManagerAction.NEXT
        val nextPendingIntent = PendingIntent
            .getService(context, 0, nextIntent, 0) // 알림
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Music Player")
            .setContentText("My Music")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true) // true 일경우 알림 리스트에서 클릭하거나 좌우로 드래그해도 사라지지 않음
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Prev", prevPendingIntent))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", playPendingIntent))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", nextPendingIntent))
            .setContentIntent(pendingIntent)
            .build()
        // Oreo 부터는 Notification Channel을 만들어야 함
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Music Player Channel", // 채널표시명
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
        return notification
    }
}


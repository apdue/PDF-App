package com.mypdf.ocrpdfapp.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mypdf.ocrpdfapp.R

class PdfForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
    }

    private fun createNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "pdf_editor_service")
            .setContentTitle("PDF Viewer")
            .setContentText("Tap to open the PDF Viewer")
            .setSmallIcon(R.drawable.ic_notification) // Replace with your icon
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Ensures it can't be swiped away
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Ensures service restarts if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
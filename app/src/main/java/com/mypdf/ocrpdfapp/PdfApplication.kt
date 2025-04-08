package com.mypdf.ocrpdfapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mypdf.ocrpdfapp.service.PdfForegroundService
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PdfApplication : Application() {


    companion object {
        lateinit var applicationContext: PdfApplication


        fun getAppContext(): PdfApplication {
            if (applicationContext == null) {
                applicationContext = PdfApplication()
            }
            return applicationContext
        }
    }
    private val CHANNEL_ID = "pdf_editor_service"

    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox
        Companion.applicationContext = this
        PDFBoxResourceLoader.init(applicationContext)
        createNotificationChannel(this)

        // Start the foreground service
        startForegroundService()
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PDF Viewer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "This notification keeps the PDF Viewer service active."
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        Log.d("TAGNOTI", "startForegroundService: ")

        val serviceIntent = Intent(this, PdfForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
} 
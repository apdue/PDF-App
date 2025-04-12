package com.mypdf.ocrpdfapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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

    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox
        Companion.applicationContext = this
        PDFBoxResourceLoader.init(applicationContext)
    }


} 
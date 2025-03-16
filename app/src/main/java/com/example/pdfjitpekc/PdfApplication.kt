package com.example.pdfjitpekc

import android.app.Application
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
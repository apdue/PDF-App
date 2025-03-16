package com.example.pdfjitpekc.model

import android.net.Uri
import java.io.File

data class PdfFile(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val file: File?,
    val uri: Uri? = null
) 
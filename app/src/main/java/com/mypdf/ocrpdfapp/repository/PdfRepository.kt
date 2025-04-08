package com.mypdf.ocrpdfapp.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.mypdf.ocrpdfapp.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfRepository(private val context: Context) {
    private var cachedPdfFiles: List<PdfFile>? = null

    suspend fun getAllPdfFiles(): List<PdfFile> = withContext(Dispatchers.IO) {
        // Return cached files if available
        cachedPdfFiles?.let { return@withContext it }

        val pdfFiles = mutableListOf<PdfFile>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val path = cursor.getString(pathColumn)
                    val file = File(path)
                    
                    // Skip if file doesn't exist or can't be read
                    if (!file.exists() || !file.canRead()) continue

                    val contentUri = Uri.withAppendedPath(collection, id.toString())
                    
                    pdfFiles.add(
                        PdfFile(
                            name = cursor.getString(nameColumn),
                            path = path,
                            size = cursor.getLong(sizeColumn),
                            lastModified = cursor.getLong(dateColumn) * 1000,
                            file = file,
                            uri = contentUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Cache the results
        cachedPdfFiles = pdfFiles
        pdfFiles
    }

    fun clearCache() {
        cachedPdfFiles = null
    }
} 
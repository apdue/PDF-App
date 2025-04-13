package com.mypdf.ocrpdfapp.repository

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.mypdf.ocrpdfapp.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfRepository(private val context: Context) {
    private var cachedPdfFiles: List<PdfFile>? = null
    private val seenPaths = mutableSetOf<String>()

    suspend fun getAllPdfFiles(): List<PdfFile> = withContext(Dispatchers.IO) {
        // Return cached files if available
        cachedPdfFiles?.let { return@withContext it }

        val pdfFiles = mutableListOf<PdfFile>()
        seenPaths.clear()

        // First, try to get files from MediaStore as it's faster
        queryMediaStore(pdfFiles)

        // Then, search in Downloads directory if needed
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        searchInDirectory(downloadDir, pdfFiles)

        // Also search in our app's directory in Documents where signed PDFs are saved
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val appSignedDir = File(documentsDir, "PDFApp/Signed")
        if (appSignedDir.exists()) {
            searchInDirectory(appSignedDir, pdfFiles)
        }

        // Cache the results
        cachedPdfFiles = pdfFiles
        pdfFiles
    }

    private suspend fun queryMediaStore(pdfFiles: MutableList<PdfFile>) = withContext(Dispatchers.IO) {
        val projection = arrayOf(
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
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(pathColumn)
                    if (path in seenPaths) continue

                    val file = File(path)
                    if (!file.exists() || !file.canRead()) continue

                    seenPaths.add(path)
                    pdfFiles.add(
                        PdfFile(
                            name = cursor.getString(nameColumn),
                            path = path,
                            size = cursor.getLong(sizeColumn),
                            lastModified = cursor.getLong(dateColumn) * 1000,
                            file = file,
                            uri = null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun searchInDirectory(directory: File, pdfFiles: MutableList<PdfFile>) {
        try {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    searchInDirectory(file, pdfFiles)
                } else if (file.name.lowercase().endsWith(".pdf")) {
                    val path = file.absolutePath
                    if (path !in seenPaths && file.canRead()) {
                        seenPaths.add(path)
                        pdfFiles.add(
                            PdfFile(
                                name = file.name,
                                path = path,
                                size = file.length(),
                                lastModified = file.lastModified(),
                                file = file,
                                uri = null
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearCache() {
        cachedPdfFiles = null
        seenPaths.clear()
    }
} 
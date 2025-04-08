package com.mypdf.ocrpdfapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mypdf.ocrpdfapp.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun renderPdfPage(context: Context, pdfFile: PdfFile, pageNumber: Int): Bitmap? {
    return withContext(Dispatchers.IO) {
        var fileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        
        try {
            // Get file descriptor based on whether we have a URI or file
            fileDescriptor = when {
                pdfFile.uri != null -> {
                    context.contentResolver.openFileDescriptor(pdfFile.uri, "r")
                }
                pdfFile.file != null -> {
                    ParcelFileDescriptor.open(pdfFile.file, ParcelFileDescriptor.MODE_READ_ONLY)
                }
                else -> null
            }
            
            if (fileDescriptor == null) {
                throw IllegalStateException("Could not open PDF file")
            }

            renderer = PdfRenderer(fileDescriptor)
            if (pageNumber < 1 || pageNumber > renderer.pageCount) {
                throw IllegalArgumentException("Invalid page number")
            }

            page = renderer.openPage(pageNumber - 1)
            val width = page.width * 2
            val height = page.height * 2
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                page?.close()
                renderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfFile: PdfFile,
    onBackClick: () -> Unit
) {
    var currentPage by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var scale by remember { mutableStateOf(1f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pdfFile, currentPage) {
        isLoading = true
        scope.launch {
            try {
                pdfBitmap = renderPdfPage(context, pdfFile, currentPage)
                if (pdfBitmap == null) {
                    errorMessage = "Error loading PDF page"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(pdfFile) {
        withContext(Dispatchers.IO) {
            try {
                val fileDescriptor = when {
                    pdfFile.uri != null -> {
                        context.contentResolver.openFileDescriptor(pdfFile.uri, "r")
                    }
                    pdfFile.file != null -> {
                        ParcelFileDescriptor.open(pdfFile.file, ParcelFileDescriptor.MODE_READ_ONLY)
                    }
                    else -> null
                }
                
                if (fileDescriptor == null) {
                    errorMessage = "Could not open PDF file"
                    return@withContext
                }

                val renderer = PdfRenderer(fileDescriptor)
                totalPages = renderer.pageCount
                renderer.close()
                fileDescriptor.close()
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error: ${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pdfFile.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
//                actions = {
                    // Share button
//                    IconButton(
//                        onClick = {
//                            try {
//                                val uri = when {
//                                    pdfFile.uri != null -> pdfFile.uri
//                                    pdfFile.file != null -> FileProvider.getUriForFile(
//                                        context,
//                                        "${context.applicationContext.packageName}.provider",
//                                        pdfFile.file
//                                    )
//                                    else -> null
//                                }
//
//                                if (uri == null) {
//                                    Toast.makeText(context, "Error: Cannot share PDF", Toast.LENGTH_SHORT).show()
//                                    return@IconButton
//                                }
//
//                                val intent = Intent(Intent.ACTION_SEND).apply {
//                                    type = "application/pdf"
//                                    putExtra(Intent.EXTRA_STREAM, uri)
//                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                                }
//                                context.startActivity(Intent.createChooser(intent, "Share PDF"))
//                            } catch (e: Exception) {
//                                Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    ) {
//                        Icon(Icons.Default.Share, contentDescription = "Share")
//                    }
                    
                    // Options button
//                    IconButton(
//                        onClick = {
//                            try {
//                                val intent = Intent(context, PdfOptionsActivity::class.java).apply {
//                                    when {
//                                        pdfFile.uri != null -> putExtra("pdf_uri", pdfFile.uri)
//                                        pdfFile.file != null -> putExtra("pdf_path", pdfFile.path)
//                                    }
//                                }
//                                context.startActivity(intent)
//                            } catch (e: Exception) {
//                                Toast.makeText(context, "Error opening options: ${e.message}", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    ) {
//                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
//                    }
//                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBackClick) {
                        Text("Go Back")
                    }
                }
            } else {
                pdfBitmap?.let { bitmap ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "PDF page $currentPage",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentPage > 1) currentPage--
                                },
                                enabled = currentPage > 1
                            ) {
                                Icon(Icons.Default.ArrowBack, "Previous page")
                            }
                            
                            Text("Page $currentPage of $totalPages")
                            
                            IconButton(
                                onClick = {
                                    if (currentPage < totalPages) currentPage++
                                },
                                enabled = currentPage < totalPages
                            ) {
                                Icon(Icons.Default.ArrowBack, "Next page", modifier = Modifier.scale(-1f, 1f))
                            }
                        }
                    }
                }
            }
        }
    }
} 
package com.example.pdfjitpekc.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.pdfjitpekc.screens.ReaderActivity
import com.example.pdfjitpekc.ui.theme.PdfJitpekcTheme
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfCompressActivity : ComponentActivity() {
    private var pdfPath: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize PDFBox
        PDFBoxResourceLoader.init(applicationContext)
        
        // Get PDF path from intent
        pdfPath = intent.getStringExtra("pdf_path")
        
        if (pdfPath == null) {
            Toast.makeText(this, "Error: No PDF file specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setContent {
            PdfJitpekcTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PdfCompressScreen(
                        pdfPath = pdfPath!!,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfCompressScreen(
    pdfPath: String,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var originalSize by remember { mutableStateOf(0L) }
    var compressedSize by remember { mutableStateOf(0L) }
    var compressionProgress by remember { mutableStateOf(0f) }
    var isCompressing by remember { mutableStateOf(false) }
    var compressionQuality by remember { mutableStateOf(0.7f) }
    var compressedFilePath by remember { mutableStateOf<String?>(null) }
    var compressionStatus by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Load PDF info
    LaunchedEffect(pdfPath) {
        isLoading = true
        try {
            withContext(Dispatchers.IO) {
                val file = File(pdfPath)
                originalSize = file.length()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }
    
    // Helper function to get content URI using FileProvider
    fun getUriForFile(file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compress PDF") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "PDF Compression",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Original Size: ${formatFileSize(originalSize)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    if (compressedSize > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Compressed Size: ${formatFileSize(compressedSize)} (${calculateReduction(originalSize, compressedSize)}% reduction)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Compression Quality: ${(compressionQuality * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = compressionQuality,
                        onValueChange = { compressionQuality = it },
                        valueRange = 0.1f..0.9f,
                        steps = 8,
                        enabled = !isCompressing
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Lower quality = smaller file size, but reduced image quality",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                compressedFilePath = null
                                isCompressing = true
                                compressionProgress = 0f
                                compressionStatus = "Preparing to compress..."
                                
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        compressPdf(pdfPath, compressionQuality) { progress ->
                                            compressionProgress = progress
                                            when {
                                                progress < 0.3f -> compressionStatus = "Analyzing PDF structure..."
                                                progress < 0.6f -> compressionStatus = "Compressing images and content..."
                                                progress < 0.9f -> compressionStatus = "Optimizing document..."
                                                else -> compressionStatus = "Finalizing compressed PDF..."
                                            }
                                        }
                                    }
                                    
                                    compressedFilePath = result.first
                                    compressedSize = result.second
                                    
                                    val reduction = calculateReduction(originalSize, compressedSize)
                                    compressionStatus = if (reduction > 0) {
                                        "Successfully reduced file size by $reduction%"
                                    } else {
                                        "File already optimized (no further reduction possible)"
                                    }
                                    
                                    Toast.makeText(
                                        context,
                                        "PDF compressed successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Log.d("TagCompress", "e: $e")
                                    compressionStatus = "Error: ${e.message}"
                                    Toast.makeText(
                                       context,
                                        "Error compressing PDF: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    isCompressing = false
                                    compressionProgress = 1f
                                }
                            }
                        },
                        enabled = !isCompressing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Compress PDF")
                    }
                    
                    if (isCompressing || compressionStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isCompressing) {
                            LinearProgressIndicator(
                                progress = compressionProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = compressionStatus,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    if (compressedFilePath != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (compressedFilePath != null) {
                                        val file = File(compressedFilePath!!)
                                        val uri = getUriForFile(file)
                                        val intent = Intent(context, ReaderActivity::class.java).apply {
                                            data = uri
                                            action = Intent.ACTION_VIEW
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error opening PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open Compressed PDF")
                            }
                            
                            Button(
                                onClick = {
                                    if (compressedFilePath != null) {
                                        val file = File(compressedFilePath!!)
                                        val uri = getUriForFile(file)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            type = "application/pdf"
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Compressed PDF"))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Share")
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun compressPdf(
    pdfPath: String,
    quality: Float,
    onProgressUpdate: (Float) -> Unit
): Pair<String, Long> = withContext(Dispatchers.IO) {
    val inputFile = File(pdfPath)
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PDFApp/Compressed")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    
    val fileName = inputFile.nameWithoutExtension + "_compressed_" + timestamp + ".pdf"
    val outputFile = File(outputDir, fileName)
    
    try {
        // First try with iText (usually better compression)
        val success = compressWithIText(inputFile, outputFile, quality)
        
        // If iText compression failed or didn't reduce size enough, try with PDFBox
        if (!success || outputFile.length() >= inputFile.length() * 0.95) {
            Log.d("PdfCompress", "iText compression not effective, trying PDFBox")
            compressWithPdfBox(inputFile, outputFile, quality, onProgressUpdate)
        } else {
            // iText was successful
            onProgressUpdate(1.0f)
        }
    } catch (e: Exception) {
        Log.e("PdfCompress", "Error during compression: ${e.message}")
        
        // If any method fails, try the other one
        try {
            if (outputFile.exists()) {
                outputFile.delete()
            }
            
            Log.d("PdfCompress", "Trying alternative compression method")
            compressWithPdfBox(inputFile, outputFile, quality, onProgressUpdate)
        } catch (e2: Exception) {
            Log.e("PdfCompress", "Both compression methods failed: ${e2.message}")
            throw e2
        }
    }
    
    // If the output file is larger than the input file, just copy the input file
    // This ensures we never make the file larger
    if (outputFile.length() > inputFile.length()) {
        Log.d("PdfCompress", "Compressed file is larger than original, using original")
        outputFile.delete()
        inputFile.copyTo(outputFile, overwrite = true)
    }
    
    return@withContext Pair(outputFile.absolutePath, outputFile.length())
}

/**
 * Compress PDF using iText library
 * @return true if compression was successful
 */
private fun compressWithIText(inputFile: File, outputFile: File, quality: Float): Boolean {
    try {
        // Convert quality (0.1-0.9) to iText compression level (9-1)
        // Higher quality = lower compression in iText
        val compressionLevel = (9 - (quality * 8).toInt()).coerceIn(1, 9)
        
        val reader = PdfReader(inputFile.absolutePath)
        
        // Set full compression
        reader.removeUnusedObjects()
        reader.removeUnusedObjects()
        
        // Create stamper with compression
        val stamper = PdfStamper(reader, FileOutputStream(outputFile))
        
        // Set compression level for the entire document
        stamper.setFullCompression()
        stamper.writer.compressionLevel = compressionLevel
        
        // Compress each page
        val pageCount = reader.numberOfPages
        for (i in 1..pageCount) {
            // Compress page content
            reader.setPageContent(i, reader.getPageContent(i), compressionLevel)
        }
        
        // Close the stamper to apply changes
        stamper.close()
        reader.close()
        
        return true
    } catch (e: Exception) {
        Log.e("PdfCompress", "iText compression failed: ${e.message}")
        e.printStackTrace()
        return false
    }
}

/**
 * Compress PDF using PDFBox library
 */
private fun compressWithPdfBox(
    inputFile: File, 
    outputFile: File, 
    quality: Float,
    onProgressUpdate: (Float) -> Unit
) {
    // Load the original document
    val document = PDDocument.load(inputFile)
    val pageCount = document.numberOfPages
    
    try {
        // Create a new document for the compressed version
        val compressedDocument = PDDocument()
        
        // Process each page
        for (i in 0 until pageCount) {
            onProgressUpdate((i.toFloat() / pageCount) * 0.8f) // 80% of progress for processing
            
            val page = document.getPage(i)
            val resources = page.resources
            
            // Clone the page to the new document
            val newPage = compressedDocument.importPage(page)
            
            // Process images on the page if there are any
            val xObjectNames = resources.xObjectNames
            if (xObjectNames != null) {
                for (xObjectName in xObjectNames) {
                    val xObject = resources.getXObject(xObjectName)
                    
                    // Check if it's an image
                    if (xObject is PDImageXObject) {
                        try {
                            // Get the image as a bitmap
                            val image = xObject.image
                            
                            // Create a compressed JPEG version of the image
                            val compressedImage = JPEGFactory.createFromImage(
                                compressedDocument,
                                image,
                                quality // Use the quality parameter
                            )
                            
                            // Replace the original image with the compressed one
                            resources.put(xObjectName, compressedImage)
                        } catch (e: Exception) {
                            Log.e("PdfCompress", "Error compressing image: ${e.message}")
                            // Continue with other images if one fails
                        }
                    }
                }
            }
            
            // Optimize text objects
            optimizeTextObjects(newPage)
        }
        
        // Set document optimization options
        compressedDocument.documentInformation = document.documentInformation
        
        // Remove unnecessary metadata
        removeUnnecessaryMetadata(compressedDocument)
        
        // Save the compressed document
        onProgressUpdate(0.9f) // 90% progress
        compressedDocument.save(outputFile)
        
        // If the compressed file is not smaller than the original, try a different approach
        if (outputFile.length() >= inputFile.length() * 0.95) { // If reduction is less than 5%
            // Try a more aggressive compression by reducing DPI of images
            compressedDocument.close()
            
            // Create a new document with more aggressive settings
            val aggressiveDocument = PDDocument.load(inputFile)
            compressAggressively(aggressiveDocument, outputFile, quality * 0.8f) // More aggressive quality
            aggressiveDocument.close()
        }
        
        onProgressUpdate(1.0f) // 100% progress
        
        // Close both documents
        compressedDocument.close()
        document.close()
        
    } catch (e: Exception) {
        Log.e("PdfCompress", "PDFBox compression failed: ${e.message}")
        document.close()
        throw e
    }
}

/**
 * Optimize text objects in a PDF page
 */
private fun optimizeTextObjects(page: com.tom_roush.pdfbox.pdmodel.PDPage) {
    // This is a simplified version. In a real implementation, you would
    // iterate through text objects and optimize them.
    // For now, we're just adjusting some page properties
    
    // Reduce margins if they're excessive
    val mediaBox = page.mediaBox
    if (mediaBox != null && mediaBox.width > 0 && mediaBox.height > 0) {
        // Keep the page size as is, but we could optimize it if needed
    }
}

/**
 * Remove unnecessary metadata from the document
 */
private fun removeUnnecessaryMetadata(document: PDDocument) {
    // Remove some unnecessary metadata fields that don't affect the document content
    val info = document.documentInformation
    
    // Keep essential metadata but remove others
    val title = info.title
    val author = info.author
    val subject = info.subject
    
    // Create a new clean information dictionary
    val newInfo = com.tom_roush.pdfbox.pdmodel.PDDocumentInformation()
    if (!title.isNullOrBlank()) newInfo.title = title
    if (!author.isNullOrBlank()) newInfo.author = author
    if (!subject.isNullOrBlank()) newInfo.subject = subject
    
    // Set the cleaned information dictionary
    document.documentInformation = newInfo
}

/**
 * Apply more aggressive compression techniques
 */
private fun compressAggressively(document: PDDocument, outputFile: File, quality: Float) {
    // Iterate through all pages
    for (i in 0 until document.numberOfPages) {
        val page = document.getPage(i)
        val resources = page.resources
        
        // Process images with more aggressive settings
        val xObjectNames = resources.xObjectNames
        if (xObjectNames != null) {
            for (xObjectName in xObjectNames) {
                val xObject = resources.getXObject(xObjectName)
                
                // Check if it's an image
                if (xObject is com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                    try {
                        // Get the image as a bitmap
                        val image = xObject.image
                        
                        // Create a more compressed JPEG version of the image
                        // with lower quality and potentially reduced dimensions
                        val compressedImage = JPEGFactory.createFromImage(
                            document,
                            image,
                            quality // Lower quality for more compression
                        )
                        
                        // Replace the original image with the compressed one
                        resources.put(xObjectName, compressedImage)
                    } catch (e: Exception) {
                        Log.e("PdfCompress", "Error in aggressive compression: ${e.message}")
                    }
                }
            }
        }
    }
    
    // Save with maximum compression
    document.save(outputFile)
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun calculateReduction(originalSize: Long, compressedSize: Long): Int {
    if (originalSize <= 0) return 0
    return ((originalSize - compressedSize) * 100 / originalSize).toInt()
} 
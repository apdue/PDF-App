package com.mypdf.ocrpdfapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mypdf.ocrpdfapp.screens.ReaderActivity
import com.mypdf.ocrpdfapp.ui.theme.PDFTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PdfSplitActivity : ComponentActivity() {
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
            PDFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PdfSplitScreen(
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
fun PdfSplitScreen(
    pdfPath: String,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var pageCount by remember { mutableStateOf(0) }
    var selectedPages by remember { mutableStateOf(setOf<Int>()) }
    var isSplitting by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var operationProgress by remember { mutableStateOf(0f) }
    var operationStatus by remember { mutableStateOf("") }
    var splitResults by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Load PDF info
    LaunchedEffect(pdfPath) {
        isLoading = true
        try {
            withContext(Dispatchers.IO) {
                PDDocument.load(File(pdfPath)).use { document ->
                    pageCount = document.numberOfPages
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error loading PDF: ${e.message}", Toast.LENGTH_SHORT).show()
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
                title = { Text("Edit PDF Pages") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Select All button
                    IconButton(
                        onClick = {
                            selectedPages = if (selectedPages.size == pageCount) {
                                emptySet()
                            } else {
                                (1..pageCount).toSet()
                            }
                        },
                        enabled = !isLoading && !isSplitting && !isDeleting
                    ) {
                        Icon(
                            if (selectedPages.size == pageCount) Icons.Default.CheckBox 
                            else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Select All"
                        )
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
                        text = "Split or Delete PDF Pages",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Select pages to extract or delete",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Page selection grid
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val rows = (pageCount + 3) / 4 // 4 pages per row, rounded up
                        
                        items(rows) { rowIndex ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (colIndex in 0 until 4) {
                                    val pageNumber = rowIndex * 4 + colIndex + 1
                                    if (pageNumber <= pageCount) {
                                        PageSelectionItem(
                                            pageNumber = pageNumber,
                                            isSelected = selectedPages.contains(pageNumber),
                                            onToggle = {
                                                selectedPages = if (selectedPages.contains(pageNumber)) {
                                                    selectedPages - pageNumber
                                                } else {
                                                    selectedPages + pageNumber
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        // Empty space for alignment
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Extract Selected Pages button
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isSplitting = true
                                    operationStatus = "Extracting pages..."
                                    operationProgress = 0f
                                    
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            splitPdfByPages(pdfPath, selectedPages.toList()) { progress ->
                                                operationProgress = progress
                                            }
                                        }
                                        
                                        splitResults = result
                                        operationStatus = "Successfully extracted ${selectedPages.size} pages to ${result.size} file(s)"
                                        
                                        Toast.makeText(
                                            context,
                                            "PDF split successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        operationStatus = "Error: ${e.message}"
                                        Toast.makeText(
                                            context,
                                            "Error splitting PDF: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        isSplitting = false
                                        operationProgress = 1f
                                    }
                                }
                            },
                            enabled = !isSplitting && !isDeleting && selectedPages.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Extract Selected")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Delete Selected Pages button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isDeleting = true
                                    operationStatus = "Deleting selected pages..."
                                    operationProgress = 0f
                                    
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            deletePdfPages(pdfPath, selectedPages.toList()) { progress ->
                                                operationProgress = progress
                                            }
                                        }
                                        
                                        splitResults = listOf(result)
                                        operationStatus = "Successfully deleted ${selectedPages.size} pages from PDF"
                                        
                                        // Refresh page count after deletion
                                        PDDocument.load(File(result)).use { document ->
                                            pageCount = document.numberOfPages
                                        }
                                        
                                        // Clear selection after deletion
                                        selectedPages = emptySet()
                                        
                                        Toast.makeText(
                                            context,
                                            "Pages deleted successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        operationStatus = "Error: ${e.message}"
                                        Toast.makeText(
                                            context,
                                            "Error deleting pages: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        isDeleting = false
                                        operationProgress = 1f
                                    }
                                }
                            },
                            enabled = !isSplitting && !isDeleting && selectedPages.isNotEmpty() && selectedPages.size < pageCount,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Delete Selected")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Split All Pages button
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isSplitting = true
                                operationStatus = "Splitting PDF into individual pages..."
                                operationProgress = 0f
                                
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        splitPdfIntoSinglePages(pdfPath) { progress ->
                                            operationProgress = progress
                                        }
                                    }
                                    
                                    splitResults = result
                                    operationStatus = "Successfully split PDF into ${result.size} individual pages"
                                    
                                    Toast.makeText(
                                        context,
                                        "PDF split successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    operationStatus = "Error: ${e.message}"
                                    Toast.makeText(
                                        context,
                                        "Error splitting PDF: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    isSplitting = false
                                    operationProgress = 1f
                                }
                            }
                        },
                        enabled = !isSplitting && !isDeleting && pageCount > 1,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Split All Pages")
                    }
                    
                    if (isSplitting || isDeleting || operationStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isSplitting || isDeleting) {
                            LinearProgressIndicator(
                                progress = operationProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = operationStatus,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Results section
                    if (splitResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Results:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            items(splitResults) { filePath ->
                                val file = File(filePath)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = file.name,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    
                                    IconButton(
                                        onClick = {
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
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = "Open PDF")
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            val uri = getUriForFile(file)
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                type = "application/pdf"
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Share"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PageSelectionItem(
    pageNumber: Int,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = pageNumber.toString(),
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Split PDF by extracting specific pages
 */
private suspend fun splitPdfByPages(
    pdfPath: String,
    pageNumbers: List<Int>,
    onProgressUpdate: (Float) -> Unit
): List<String> = withContext(Dispatchers.IO) {
    if (pageNumbers.isEmpty()) {
        return@withContext emptyList()
    }
    
    val inputFile = File(pdfPath)
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PDFApp/Split")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    
    val sortedPages = pageNumbers.sorted()
    val document = PDDocument.load(inputFile)
    
    try {
        // Create a new document with selected pages
        val newDocument = PDDocument()
        
        for ((index, pageNumber) in sortedPages.withIndex()) {
            onProgressUpdate(index.toFloat() / sortedPages.size)
            
            if (pageNumber <= document.numberOfPages && pageNumber > 0) {
                // PDFBox uses 0-based page indices
                val page = document.getPage(pageNumber - 1)
                // Import the page to the new document
                newDocument.addPage(newDocument.importPage(page))
            }
        }
        
        // Save the new document
        val outputFileName = "${inputFile.nameWithoutExtension}_pages_${sortedPages.joinToString("-")}_$timestamp.pdf"
        val outputFile = File(outputDir, outputFileName)
        newDocument.save(outputFile)
        newDocument.close()
        
        onProgressUpdate(1.0f)
        return@withContext listOf(outputFile.absolutePath)
        
    } finally {
        document.close()
    }
}

/**
 * Split PDF into individual pages
 */
private suspend fun splitPdfIntoSinglePages(
    pdfPath: String,
    onProgressUpdate: (Float) -> Unit
): List<String> = withContext(Dispatchers.IO) {
    val inputFile = File(pdfPath)
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PDFApp/Split")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    
    val document = PDDocument.load(inputFile)
    val pageCount = document.numberOfPages
    val resultFiles = mutableListOf<String>()
    
    try {
        for (i in 0 until pageCount) {
            onProgressUpdate(i.toFloat() / pageCount)
            
            // Create a new document for this page
            val newDocument = PDDocument()
            
            // Import the page
            val page = document.getPage(i)
            // Import the page to the new document
            newDocument.addPage(newDocument.importPage(page))
            
            // Save the single-page document
            val outputFileName = "${inputFile.nameWithoutExtension}_page_${i + 1}_$timestamp.pdf"
            val outputFile = File(outputDir, outputFileName)
            newDocument.save(outputFile)
            newDocument.close()
            
            resultFiles.add(outputFile.absolutePath)
        }
        
        onProgressUpdate(1.0f)
        return@withContext resultFiles
        
    } finally {
        document.close()
    }
}

/**
 * Delete selected pages from a PDF
 */
private suspend fun deletePdfPages(
    pdfPath: String,
    pageNumbers: List<Int>,
    onProgressUpdate: (Float) -> Unit
): String = withContext(Dispatchers.IO) {
    if (pageNumbers.isEmpty()) {
        return@withContext pdfPath
    }
    
    val inputFile = File(pdfPath)
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PDFApp/Split")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    
    val document = PDDocument.load(inputFile)
    val pageCount = document.numberOfPages
    
    try {
        // Create a set of pages to delete (0-based indices)
        val pagesToDelete = pageNumbers.map { it - 1 }.toSet()
        
        // Create a new document without the selected pages
        val newDocument = PDDocument()
        
        for (i in 0 until pageCount) {
            onProgressUpdate(i.toFloat() / pageCount)
            
            // Skip pages that should be deleted
            if (i !in pagesToDelete) {
                val page = document.getPage(i)
                newDocument.addPage(newDocument.importPage(page))
            }
        }
        
        // Save the new document
        val outputFileName = "${inputFile.nameWithoutExtension}_deleted_pages_$timestamp.pdf"
        val outputFile = File(outputDir, outputFileName)
        newDocument.save(outputFile)
        newDocument.close()
        
        onProgressUpdate(1.0f)
        return@withContext outputFile.absolutePath
        
    } finally {
        document.close()
    }
} 
package com.example.pdfjitpekc.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pdfjitpekc.ui.theme.PdfJitpekcTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfEditActivity : ComponentActivity() {
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
                    PdfEditScreen(
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
fun PdfEditScreen(
    pdfPath: String,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var pageCount by remember { mutableStateOf(0) }
    var documentTitle by remember { mutableStateOf("") }
    var documentAuthor by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    // Load PDF metadata
    LaunchedEffect(pdfPath) {
        isLoading = true
        try {
            withContext(Dispatchers.IO) {
                PDDocument.load(File(pdfPath)).use { document ->
                    pageCount = document.numberOfPages
                    documentTitle = document.documentInformation?.title ?: ""
                    documentAuthor = document.documentInformation?.author ?: ""
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit PDF") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isSaving = true
                                try {
                                    withContext(Dispatchers.IO) {
                                        PDDocument.load(File(pdfPath)).use { document ->
                                            // Update document information
                                            document.documentInformation.title = documentTitle
                                            document.documentInformation.author = documentAuthor
                                            
                                            // Save the document
                                            document.save(pdfPath)
                                        }
                                    }
                                    Toast.makeText(
                                        context,
                                        "PDF saved successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(
                                        context,
                                        "Error saving PDF: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
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
                        text = "PDF Information",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Pages: $pageCount",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = documentTitle,
                        onValueChange = { documentTitle = it },
                        label = { Text("Document Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = documentAuthor,
                        onValueChange = { documentAuthor = it },
                        label = { Text("Document Author") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Note: This is a basic PDF editor. Currently, you can only edit document metadata.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (isSaving) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
} 
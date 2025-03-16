package com.example.pdfjitpekc.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pdfjitpekc.ui.theme.PdfJitpekcTheme
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.content.Intent
import androidx.compose.foundation.background

class PdfTextExtractActivity : ComponentActivity() {
    private var pdfPath: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                    PdfTextExtractScreen(
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
fun PdfTextExtractScreen(
    pdfPath: String,
    onBackClick: () -> Unit
) {
    var extractedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentSearchIndex by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(pdfPath) {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val reader = PdfReader(pdfPath)
                    val text = StringBuilder()
                    
                    for (i in 1..reader.numberOfPages) {
                        text.append(PdfTextExtractor.getTextFromPage(reader, i))
                        text.append("\n\n--- Page ${i} ---\n\n")
                    }
                    
                    reader.close()
                    extractedText = text.toString()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error extracting text: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isLoading = false
            }
        }
    }

    // Search function
    fun performSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }
        
        val results = mutableListOf<Int>()
        var lastIndex = 0
        while (true) {
            val index = extractedText.indexOf(query, lastIndex, ignoreCase = true)
            if (index == -1) break
            results.add(index)
            lastIndex = index + query.length
        }
        searchResults = results
        currentSearchIndex = if (results.isNotEmpty()) 0 else -1
    }

    // Auto-scroll to current search result
    LaunchedEffect(currentSearchIndex) {
        if (currentSearchIndex >= 0 && searchResults.isNotEmpty()) {
            val targetIndex = searchResults[currentSearchIndex]
            // Calculate approximate scroll position (80 characters per line)
            val approximatePosition = (targetIndex / 80f) * 16f // 16dp per line height
            scrollState.animateScrollTo(approximatePosition.toInt())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extract Text") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            TextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    performSearch(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Search in text...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Row {
                            if (searchResults.isNotEmpty()) {
                                Text(
                                    "${currentSearchIndex + 1}/${searchResults.size}",
                                    modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                                )
                            }
                            IconButton(onClick = { 
                                searchQuery = ""
                                searchResults = emptyList()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    }
                },
                singleLine = true
            )

            // Navigation buttons for search results
            if (searchResults.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            if (currentSearchIndex > 0) {
                                currentSearchIndex--
                            }
                        },
                        enabled = currentSearchIndex > 0
                    ) {
                        Text("Previous")
                    }
                    
                    Button(
                        onClick = {
                            if (currentSearchIndex < searchResults.size - 1) {
                                currentSearchIndex++
                            }
                        },
                        enabled = currentSearchIndex < searchResults.size - 1
                    ) {
                        Text("Next")
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("PDF Text", extractedText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy All")
                        }
                        
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, extractedText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share text"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Display text with highlighted search results
                    if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                        val text = extractedText
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(scrollState)
                        ) {
                            var lastIndex = 0
                            searchResults.forEachIndexed { index, startIndex ->
                                // Add text before match
                                if (startIndex > lastIndex) {
                                    Text(text.substring(lastIndex, startIndex))
                                }
                                
                                // Add highlighted match
                                val endIndex = startIndex + searchQuery.length
                                Text(
                                    text = text.substring(startIndex, endIndex),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.background(
                                        color = if (index == currentSearchIndex)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                )
                                
                                lastIndex = endIndex
                                
                                // Add remaining text after last match
                                if (index == searchResults.lastIndex && lastIndex < text.length) {
                                    Text(text.substring(lastIndex))
                                }
                            }
                        }
                    } else {
                        Text(
                            text = extractedText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(scrollState)
                        )
                    }
                }
            }
        }
    }
}

private suspend fun extractTextFromPdf(pdfPath: String): Pair<String, Int> = withContext(Dispatchers.IO) {
    val file = File(pdfPath)
    val reader = PdfReader(file.absolutePath)
    val pageCount = reader.numberOfPages
    val textBuilder = StringBuilder()
    
    try {
        for (i in 1..pageCount) {
            val pageText = PdfTextExtractor.getTextFromPage(reader, i)
            textBuilder.append("--- Page $i ---\n\n")
            textBuilder.append(pageText)
            textBuilder.append("\n\n")
        }
    } finally {
        reader.close()
    }
    
    return@withContext Pair(textBuilder.toString(), pageCount)
} 
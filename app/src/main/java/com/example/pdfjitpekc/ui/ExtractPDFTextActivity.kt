package com.example.pdfjitpekc.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pdfjitpekc.model.PdfFile
import com.example.pdfjitpekc.model.SearchResult
import com.example.pdfjitpekc.ui.theme.PdfJitpekcTheme
import com.example.pdfjitpekc.viewmodel.PdfViewModel
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import android.util.Log
import android.os.Environment
import android.content.Intent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancelAndJoin
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.pdfjitpekc.screens.ReaderActivity
import android.util.LruCache

class SearchViewModel : ViewModel() {
    
    private val TAG = "SearchViewModel"
    
    private val _allPdfFiles = MutableStateFlow<List<PdfFile>>(emptyList())
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting

    private val _extractionProgress = MutableStateFlow(0f)
    val extractionProgress: StateFlow<Float> = _extractionProgress

    private val _currentPdfNumber = MutableStateFlow(0)
    val currentPdfNumber: StateFlow<Int> = _currentPdfNumber

    private val _totalPdfCount = MutableStateFlow(0)
    val totalPdfCount: StateFlow<Int> = _totalPdfCount

    private val _isSearchEnabled = MutableStateFlow(false)
    val isSearchEnabled: StateFlow<Boolean> = _isSearchEnabled

    // Use LruCache to limit memory usage
    private val _pdfContents = object : LruCache<String, String>(
        // Set cache size to 50MB
        (50 * 1024 * 1024)
    ) {
        override fun sizeOf(key: String, value: String): Int {
            // Approximate size in bytes
            return value.length * 2
        }
    }
    
    private var extractionJob: Job? = null

    fun startExtraction(pdfFiles: List<PdfFile>) {
        if (_isExtracting.value) {
            Log.d(TAG, "Extraction already in progress, ignoring request")
            return
        }

        // Limit the number of files to process to prevent OOM
        val maxFiles = 500
        val limitedPdfFiles = if (pdfFiles.size > maxFiles) {
            Log.w(TAG, "Too many PDF files (${pdfFiles.size}), limiting to $maxFiles")
            pdfFiles.take(maxFiles)
        } else {
            pdfFiles
        }

        // Update search results immediately with the found files
        _allPdfFiles.value = limitedPdfFiles
        _searchResults.value = limitedPdfFiles.map { pdf ->
            SearchResult(file = pdf, matchCount = 0)
        }.sortedBy { it.file.name.lowercase() }
        
        // Cancel any existing job
        viewModelScope.launch {
            try {
                extractionJob?.cancelAndJoin()
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling previous job", e)
            }
        }
        
        Log.d(TAG, "Starting extraction of ${limitedPdfFiles.size} PDFs")

        extractionJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _isExtracting.value = true
                _isSearchEnabled.value = false
                _totalPdfCount.value = limitedPdfFiles.size
                _currentPdfNumber.value = 0
                _extractionProgress.value = 0f
                
                Log.d(TAG, "Extraction started: total PDFs = ${limitedPdfFiles.size}")

                // Process files in smaller batches
                val batchSize = 3
                val batches = limitedPdfFiles.chunked(batchSize)
                
                var processedCount = 0
                
                for (batch in batches) {
                    if (!isActive) {
                        Log.d(TAG, "Extraction job cancelled")
                        break
                    }
                    
                    for (pdf in batch) {
                        if (!isActive) break
                        
                        try {
                            Log.d(TAG, "Processing PDF ${processedCount + 1}/${limitedPdfFiles.size}: ${pdf.name}")
                            
                            if (!_pdfContents.get(pdf.path).isNullOrEmpty()) {
                                Log.d(TAG, "Using cached content for ${pdf.name}")
                                processedCount++
                                continue
                            }

                            var reader: PdfReader? = null
                            try {
                                reader = PdfReader(pdf.file!!.absolutePath)
                                if (!reader.isEncrypted) {
                                    val content = StringBuilder()
                                    val pageCount = reader.numberOfPages
                                    
                                    // Limit the number of pages to process per PDF
                                    val maxPages = 100
                                    val pagesToProcess = minOf(pageCount, maxPages)
                                    
                                    Log.d(TAG, "Extracting text from ${pdf.name} ($pagesToProcess/${pageCount} pages)")
                                    
                                    for (i in 1..pagesToProcess) {
                                        if (!isActive) break
                                        try {
                                            val pageText = PdfTextExtractor.getTextFromPage(reader, i)
                                            // Limit text size per page
                                            if (content.length + pageText.length <= 1_000_000) {
                                                content.append(pageText)
                                                content.append("\n")
                                            } else {
                                                Log.w(TAG, "Text limit reached for ${pdf.name}")
                                                break
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error extracting text from page $i of ${pdf.name}", e)
                                        }
                                    }
                                    
                                    if (isActive && content.isNotEmpty()) {
                                        _pdfContents.put(pdf.path, content.toString())
                                        Log.d(TAG, "Extracted ${content.length} characters from ${pdf.name}")
                                    }
                                } else {
                                    Log.d(TAG, "Skipping encrypted PDF: ${pdf.name}")
                                }
                            } finally {
                                try {
                                    reader?.close()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error closing PDF reader for ${pdf.name}", e)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing PDF: ${pdf.name}", e)
                        } finally {
                            processedCount++
                            _currentPdfNumber.value = processedCount
                            _extractionProgress.value = processedCount.toFloat() / limitedPdfFiles.size
                            Log.d(TAG, "Progress update: ${_currentPdfNumber.value}/${_totalPdfCount.value} (${(_extractionProgress.value * 100).toInt()}%)")
                        }
                    }
                    
                    // Give the system more time to breathe between batches
                    try {
                        delay(200)
                        System.gc() // Suggest garbage collection between batches
                    } catch (e: Exception) {
                        // Ignore interruption
                    }
                }
                
                Log.d(TAG, "Extraction completed: processed ${_currentPdfNumber.value}/${_totalPdfCount.value} PDFs")
                
                // Show all PDFs after extraction
                _searchResults.value = _allPdfFiles.value.map { pdf ->
                    SearchResult(file = pdf, matchCount = 0)
                }.sortedBy { it.file.name.lowercase() }
            } catch (e: Exception) {
                Log.e(TAG, "Error during extraction process", e)
            } finally {
                _isExtracting.value = false
                _isSearchEnabled.value = true
                Log.d(TAG, "Search enabled, showing ${_searchResults.value.size} PDFs")
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                Log.d(TAG, "Searching for: '$query'")
                
                if (query.isBlank()) {
                    Log.d(TAG, "Empty query, showing all PDFs")
                    // Show all PDFs when search is empty
                    _searchResults.value = _allPdfFiles.value.map { pdf ->
                        SearchResult(file = pdf, matchCount = 0)
                    }.sortedBy { it.file.name.lowercase() }
                    Log.d(TAG, "Showing ${_searchResults.value.size} PDFs")
                    return@launch
                }

                val results = mutableListOf<SearchResult>()
                
                // Process search in batches to avoid memory pressure
                val batchSize = 10
                val allFiles = _allPdfFiles.value
                val batches = allFiles.chunked(batchSize)
                
                for (batch in batches) {
                    if (!isActive) break
                    
                    for (file in batch) {
                        if (!isActive) break
                        
                        val content = _pdfContents.get(file.path)
                        if (content != null) {
                            val matchCount = if (content.contains(query, ignoreCase = true)) {
                                content.split(query, ignoreCase = true).size - 1
                            } else 0
                            results.add(SearchResult(file = file, matchCount = matchCount))
                        } else {
                            results.add(SearchResult(file = file, matchCount = 0))
                        }
                    }
                    
                    // Update results progressively
                    _searchResults.value = results.sortedWith(
                        compareByDescending<SearchResult> { it.matchCount }
                        .thenBy { it.file.name.lowercase() }
                    )
                    
                    delay(50) // Small delay to keep UI responsive
                }
                
                Log.d(TAG, "Search completed: ${results.count { it.matchCount > 0 }} PDFs with matches")
            } catch (e: Exception) {
                Log.e(TAG, "Error during search", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        extractionJob?.cancel()
        _pdfContents.evictAll()
        Log.d(TAG, "ViewModel cleared, cancelling extraction job and clearing cache")
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
class ExtractPDFTextActivity : ComponentActivity() {

    private val TAG = "SearchActivity"
    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var pdfViewModel: PdfViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "SearchActivity onCreate")
        
        // Initialize PdfViewModel properly
        pdfViewModel = PdfViewModel().apply {
            initialize(this@ExtractPDFTextActivity)
        }
        
        setContent {
            PdfJitpekcTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val pdfFiles by pdfViewModel.pdfFiles.collectAsStateWithLifecycle(initialValue = emptyList())
                    SearchScreen(
                        viewModel = searchViewModel,
                        onExtractClick = {
                            Log.d(TAG, "Extract button clicked, scanning storage for PDF files")
                            val allPdfFiles = mutableListOf<PdfFile>()
                            
                            // First add files from PdfViewModel if available
                            if (pdfFiles.isNotEmpty()) {
                                allPdfFiles.addAll(pdfFiles)
                                Log.d(TAG, "Added ${pdfFiles.size} PDFs from PdfViewModel")
                            }
                            
                            // Then scan storage for additional PDFs
                            try {
                                val externalDirs = arrayOf(
                                    Environment.getExternalStorageDirectory()
                                )
                                
                                for (dir in externalDirs) {
                                    scanDirectory(dir, allPdfFiles, maxDepth = 2)
                                }
                                
                                // Add Downloads and Documents if they exist
                                try {
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let {
                                        scanDirectory(it, allPdfFiles, maxDepth = 1)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error scanning Downloads directory", e)
                                }
                                
                                try {
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.let {
                                        scanDirectory(it, allPdfFiles, maxDepth = 1)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error scanning Documents directory", e)
                                }
                                
                                Log.d(TAG, "Found total ${allPdfFiles.size} PDF files")
                                if (allPdfFiles.isNotEmpty()) {
                                    searchViewModel.startExtraction(allPdfFiles)
                                } else {
                                    Log.e(TAG, "No PDF files found in storage")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error scanning for PDFs", e)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun scanDirectory(directory: File, pdfFiles: MutableList<PdfFile>, maxDepth: Int = 2, currentDepth: Int = 0) {
        if (currentDepth > maxDepth) return
        
        try {
            directory.listFiles()?.forEach { file ->
                try {
                    if (file.isDirectory && currentDepth < maxDepth) {
                        scanDirectory(file, pdfFiles, maxDepth, currentDepth + 1)
                    } else if (file.isFile && file.name.lowercase().endsWith(".pdf")) {
                        val pdfFile = PdfFile(
                            name = file.name,
                            path = file.absolutePath,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            file = file
                        )
                        if (!pdfFiles.any { it.path == pdfFile.path }) {
                            pdfFiles.add(pdfFile)
                            Log.d(TAG, "Found PDF: ${file.name}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing file: ${file.path}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory: ${directory.path}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "SearchActivity onResume")
        pdfViewModel.loadPdfFiles()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onExtractClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    
    val isExtracting by viewModel.isExtracting.collectAsStateWithLifecycle()
    val extractionProgress by viewModel.extractionProgress.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearchEnabled by viewModel.isSearchEnabled.collectAsStateWithLifecycle()
    val currentPdfNumber by viewModel.currentPdfNumber.collectAsStateWithLifecycle()
    val totalPdfCount by viewModel.totalPdfCount.collectAsStateWithLifecycle()
    
    Log.d("SearchScreen", "State: isExtracting=$isExtracting, results=${searchResults.size}")

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Search PDFs") },
                    navigationIcon = {
                        IconButton(onClick = { activity?.finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (!isExtracting) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = {
                            searchQuery = it
                            if (isSearchEnabled) {
                                viewModel.search(it)
                            }
                        },
                        onSearch = {
                            keyboardController?.hide()
                            if (isSearchEnabled) {
                                viewModel.search(searchQuery)
                            }
                        },
                        enabled = isSearchEnabled
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (!isSearchEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Extract PDF contents to enable search",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = onExtractClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Extract PDF Contents")
                            }
                        }
                    }
                    
                    if (searchResults.isEmpty() && isSearchEnabled) {
                        // Show empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) 
                                    "No PDFs found" 
                                else 
                                    "No matches found for '$searchQuery'",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { result ->
                                SearchResultItem(
                                    result = result,
                                    onClick = {
                                        try {
                                            val file = result.file.file
                                            if (file == null) {
                                                Toast.makeText(context, "Error: File not found", Toast.LENGTH_SHORT).show()
                                                return@SearchResultItem
                                            }

                                            val uri = try {
                                                FileProvider.getUriForFile(
                                                    context,
                                                    "${context.applicationContext.packageName}.provider",
                                                    file
                                                )
                                            } catch (e: IllegalArgumentException) {
                                                Log.e("SearchScreen", "Error getting URI for file: ${file.absolutePath}", e)
                                                Toast.makeText(context, "Error: Cannot access file", Toast.LENGTH_SHORT).show()
                                                return@SearchResultItem
                                            }
                                            
                                            val intent = Intent(context, ReaderActivity::class.java).apply {
                                                data = uri
                                                action = Intent.ACTION_VIEW
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e("SearchScreen", "Error opening PDF", e)
                                            Toast.makeText(context, "Error opening PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    ExtractionProgress(
                        progress = extractionProgress,
                        currentPdf = currentPdfNumber,
                        totalPdfs = totalPdfCount
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    enabled: Boolean
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { 
            Text(
                if (enabled) "Search in PDFs" 
                else "Extracting PDF contents..."
            ) 
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        enabled = enabled
    )
}

@Composable
fun ExtractionProgress(
    progress: Float,
    currentPdf: Int,
    totalPdfs: Int
) {
    var progressAnimation by remember { mutableStateOf(0f) }
    
    LaunchedEffect(progress) {
        try {
            animate(
                initialValue = progressAnimation,
                targetValue = progress,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = FastOutSlowInEasing
                )
            ) { value, _ ->
                progressAnimation = value
            }
        } catch (e: Exception) {
            // Fallback if animation fails
            progressAnimation = progress
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background circle
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    progress = 1f,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round
                )
                
                // Progress circle with animation
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    progress = progressAnimation.coerceIn(0f, 1f),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round
                )
                
                // Percentage text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${(progressAnimation * 100).toInt()}%",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$currentPdf of $totalPdfs PDFs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Preparing Your PDFs",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Processing PDF $currentPdf of $totalPdfs\n" +
                              "This will make searching through your documents much faster.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    LinearProgressIndicator(
                        progress = progressAnimation.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = when {
                            progressAnimation < 0.3f -> "Starting extraction..."
                            progressAnimation < 0.7f -> "Processing PDFs..."
                            progressAnimation < 1f -> "Almost done..."
                            else -> "Extraction complete!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = result.file.name,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            if (result.matchCount > 0) {
                Text(
                    text = "${result.matchCount} matches found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "PDF File",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 
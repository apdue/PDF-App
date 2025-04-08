package com.mypdf.ocrpdfapp.screens

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.util.Log
import android.util.LruCache
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itextpdf.text.pdf.PdfReader
import com.mypdf.ocrpdfapp.model.PdfFile
import com.mypdf.ocrpdfapp.ui.ExtractPDFTextActivity
import com.mypdf.ocrpdfapp.ui.PasswordDialog
import com.mypdf.ocrpdfapp.ui.PdfViewerScreen
import com.mypdf.ocrpdfapp.ui.PermissionScreen
import com.mypdf.ocrpdfapp.ui.theme.PDFTheme
import com.mypdf.ocrpdfapp.util.ComposeAdsManager
import com.mypdf.ocrpdfapp.util.FileUtils
import com.mypdf.ocrpdfapp.util.PrefManagerVideo
import com.mypdf.ocrpdfapp.util.showInterstitialAd
import com.mypdf.ocrpdfapp.viewmodel.PdfViewModel
import com.mypdf.ocrpdfapp.viewmodel.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min

enum class ViewType {
    LIST,
    GRID
}

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private val viewModel: PdfViewModel by viewModels()
    private var permissionsGranted = mutableStateOf(false)

    // Track if the PDF was opened from an external intent
    var openedFromIntent = false
        private set

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Permission result: $permissions")
        val allGranted = permissions.entries.all { it.value }
        updatePermissionStatus()
    }

    // For Android 11+, check if we have manage storage permission
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check if permission was granted
        updatePermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        viewModel.initialize(this)

        // Check if opened from intent
        openedFromIntent = intent.action == Intent.ACTION_VIEW

        // Check if permissions are already granted
        updatePermissionStatus()

        // Handle intent if opened from external source
        handleIntent(intent)

        setContent {
            val isInPdfView by viewModel.isInPdfView.collectAsStateWithLifecycle(initialValue = false)

            // Handle back press
            BackHandler(enabled = true) {
                if (isInPdfView) {
                    viewModel.clearSelectedPdf()
                } else {
                    showInterstitialAd {
                        finish()
                    }
                }
            }

            PDFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(
                        viewModel = viewModel,
                        permissionsGranted = permissionsGranted.value,
                        onRequestPermission = { requestStoragePermissions() },
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        if (permissionsGranted.value) {
            viewModel.refreshPdfFiles()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        // Check permissions again when app comes back to foreground
        updatePermissionStatus()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        openedFromIntent = intent?.action == Intent.ACTION_VIEW
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            if (uri != null) {
                try {
                    // For content URIs
                    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                        val fileName = FileUtils.getFileName(this, uri) ?: "Unknown PDF"
                        viewModel.openPdfFromUri(uri, fileName)
                    }
                    // For file URIs
                    else if (uri.scheme == "file" || uri.path != null) {
                        val path = uri.path
                        if (path != null) {
                            val file = File(path)
                            if (file.exists() && file.extension.equals("pdf", ignoreCase = true)) {
                                val pdfFile = PdfFile(
                                    name = file.name,
                                    path = file.absolutePath,
                                    size = file.length(),
                                    lastModified = file.lastModified(),
                                    file = file,
                                    uri = null
                                )
                                // Select the PDF to view it
                                viewModel.selectPdf(pdfFile)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling intent", e)
                }
            }
        }
    }

    private fun updatePermissionStatus() {
        Log.d(TAG, "Checking permissions...")
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses Environment.isExternalStorageManager()
            Environment.isExternalStorageManager()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 uses READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-Android 6 doesn't need runtime permissions
            true
        }

        Log.d(TAG, "Permission status: $hasPermission")
        val permissionChanged = permissionsGranted.value != hasPermission
        permissionsGranted.value = hasPermission

        if (permissionChanged) {
            viewModel.updatePermissionStatus(hasPermission)
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+, we need to use special Intent for all files access
            try {
                Log.d(TAG, "Requesting all files access permission (Android 11+)")
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                storagePermissionLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting manage storage permission", e)
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storagePermissionLauncher.launch(intent)
            }
        } else {
            // For older Android versions, just request READ_EXTERNAL_STORAGE
            Log.d(TAG, "Requesting regular storage permission")
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainContent(
    viewModel: PdfViewModel,
    permissionsGranted: Boolean,
    onRequestPermission: () -> Unit,
    onBackPressed: () -> Unit,
) {
    val selectedPdf by viewModel.selectedPdf.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current

    AnimatedContent(
        targetState = selectedPdf,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) with
                    fadeOut(animationSpec = tween(300))
        }
    ) { pdf ->
        when {
            pdf != null -> {
                PdfViewerScreen(
                    pdfFile = pdf,
                    onBackClick = {
                        viewModel.clearSelectedPdf()
                    }
                )
            }

            !permissionsGranted -> {
                PermissionScreen(
                    onRequestPermission = onRequestPermission
                )
            }

            else -> {
                PdfListScreen(
                    viewModel = viewModel,
                    onPdfSelected = { viewModel.selectPdf(it) },
                    onBackPressed = onBackPressed
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfListScreen(
    viewModel: PdfViewModel,
    onPdfSelected: (PdfFile) -> Unit,
    onBackPressed: () -> Unit
) {
    var showAd by remember { mutableStateOf(false) }
    var onAdClosedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isSearchVisible by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    fun showInterstitialAd(afterAdClosed: () -> Unit) {
        onAdClosedAction = afterAdClosed
        showAd = true
    }

    LaunchedEffect(showAd) {
        if (!showAd && onAdClosedAction != null) {
            onAdClosedAction?.invoke()
            onAdClosedAction = null  // Reset state after execution
        }
    }

    if (showAd) {
        ComposeAdsManager.InterstitialAd {
            showAd = false
        }
    }

    val pdfFiles by viewModel.filteredPdfFiles.collectAsStateWithLifecycle(initialValue = emptyList())
    var showSortMenu by remember { mutableStateOf(false) }
    var viewType by remember { mutableStateOf(ViewType.LIST) }
    var showPasswordDialog by remember { mutableStateOf<Pair<PdfFile, Boolean>?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("PDF Viewer") },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { isSearchVisible = !isSearchVisible }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search PDF Files"
                            )
                        }
                        if (PrefManagerVideo(context).getString(SplashActivity.enable_extract_feature)
                                .contains("true")
                        ) {
                            IconButton(
                                onClick = {
                                    val intent = Intent(context, ExtractPDFTextActivity::class.java)
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = "Extract Text"
                                )
                            }
                        }

                        // View type toggle
                        IconButton(
                            onClick = {
                                viewType =
                                    if (viewType == ViewType.LIST) ViewType.GRID else ViewType.LIST
                            }
                        ) {
                            Icon(
                                imageVector = if (viewType == ViewType.LIST)
                                    Icons.Default.Menu else Icons.Default.List,
                                contentDescription = "Toggle view"
                            )
                        }
                        // Sort menu
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest First") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.NEWEST_FIRST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest First") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.OLDEST_FIRST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Largest First") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.LARGEST_FIRST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Smallest First") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.SMALLEST_FIRST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.NAME_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.NAME_DESC)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                )
                if (isSearchVisible) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search PDF files...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        } else null
                    )
                }
            }
        }
    ) { paddingValues ->
        if (viewModel.isLoading.collectAsStateWithLifecycle(initialValue = true).value) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (pdfFiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No PDF files found")
            }
        } else {
            when (viewType) {
                ViewType.LIST -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        item {
                            ComposeAdsManager.NativeAd()
                        }

                        items(pdfFiles) { pdf ->
                            PdfListItem(
                                pdf = pdf,
                                onClick = {
                                    checkPdfProtection(pdf) { isProtected ->
                                        if (isProtected) {
                                            showPasswordDialog = pdf to false
                                        } else {

                                            showInterstitialAd {
                                                // Launch PdfOptionsActivity instead of directly opening the PDF
                                                val intent = Intent(
                                                    context,
                                                    PdfOptionsActivity::class.java
                                                ).apply {
                                                    if (pdf.file != null) {
                                                        putExtra("pdf_path", pdf.file.absolutePath)
                                                    }
                                                }
                                                context.startActivity(intent)
                                            }

                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                ViewType.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pdfFiles) { pdf ->
                            PdfGridItem(
                                pdf = pdf,
                                onClick = {
                                    checkPdfProtection(pdf) { isProtected ->
                                        if (isProtected) {
                                            showPasswordDialog = pdf to false
                                        } else {

                                            showInterstitialAd {
                                                val intent = Intent(
                                                    context,
                                                    PdfOptionsActivity::class.java
                                                ).apply {
                                                    if (pdf.file != null) {
                                                        putExtra("pdf_path", pdf.file.absolutePath)
                                                    }
                                                }
                                                context.startActivity(intent)
                                            }

                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Password Dialog
        showPasswordDialog?.let { (pdf, isError) ->
            PasswordDialog(
                onDismiss = { showPasswordDialog = null },
                onPasswordEntered = { password ->
                    try {
                        val reader = PdfReader(pdf.file!!.absolutePath, password.toByteArray())
                        reader.close()
                        showPasswordDialog = null

                        // Launch PdfOptionsActivity instead of directly opening the PDF
                        val intent = Intent(context, PdfOptionsActivity::class.java).apply {
                            if (pdf.file != null) {
                                putExtra("pdf_path", pdf.file.absolutePath)
                            }
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        showPasswordDialog = pdf to true
                    }
                }
            )
        }
    }
}

private fun checkPdfProtection(pdf: PdfFile, onResult: (Boolean) -> Unit) {
    try {
        if (pdf.file != null) {
            val reader = PdfReader(pdf.file.absolutePath)
            val isProtected = reader.isEncrypted
            reader.close()
            onResult(isProtected)
        } else {
            onResult(false) // Assume not protected for URI-based PDFs
        }
    } catch (e: Exception) {
        onResult(true) // Assume protected if can't read
    }
}

object ThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Use 1/8th of available memory for cache

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun addBitmapToCache(key: String, bitmap: Bitmap) {
        if (getBitmapFromCache(key) == null) {
            memoryCache.put(key, bitmap)
        }
    }

    fun getBitmapFromCache(key: String): Bitmap? {
        return memoryCache.get(key)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfListItem(
    pdf: PdfFile,
    onClick: () -> Unit
) {
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pdf) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Try to get from cache first
                thumbnail = ThumbnailCache.getBitmapFromCache(pdf.path)

                if (thumbnail == null) {
                    val pdfRenderer = PdfRenderer(
                        ParcelFileDescriptor.open(pdf.file, ParcelFileDescriptor.MODE_READ_ONLY)
                    )

                    if (pdfRenderer.pageCount > 0) {
                        val firstPage = pdfRenderer.openPage(0)
                        // Create a smaller bitmap for thumbnail
                        val width = min(firstPage.width, 200)
                        val height =
                            (width * (firstPage.height.toFloat() / firstPage.width)).toInt()
                        val bitmap = Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888
                        )
                        firstPage.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )
                        firstPage.close()
                        pdfRenderer.close()

                        // Cache the thumbnail
                        ThumbnailCache.addBitmapToCache(pdf.path, bitmap)
                        thumbnail = bitmap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PDF Thumbnail
            Card(
                modifier = Modifier.size(100.dp)
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = "PDF Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pdf.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = FileUtils.formatFileSize(pdf.size),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = FileUtils.formatDate(pdf.lastModified),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfGridItem(
    pdf: PdfFile,
    onClick: () -> Unit
) {
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pdf) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Try to get from cache first
                thumbnail = ThumbnailCache.getBitmapFromCache(pdf.path)

                if (thumbnail == null) {
                    val pdfRenderer = PdfRenderer(
                        ParcelFileDescriptor.open(pdf.file, ParcelFileDescriptor.MODE_READ_ONLY)
                    )

                    if (pdfRenderer.pageCount > 0) {
                        val firstPage = pdfRenderer.openPage(0)
                        // Create a smaller bitmap for thumbnail
                        val width = min(firstPage.width, 300)
                        val height =
                            (width * (firstPage.height.toFloat() / firstPage.width)).toInt()
                        val bitmap = Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888
                        )
                        firstPage.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )
                        firstPage.close()
                        pdfRenderer.close()

                        // Cache the thumbnail
                        ThumbnailCache.addBitmapToCache(pdf.path, bitmap)
                        thumbnail = bitmap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thumbnail area (60% of height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = "PDF Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Info area (40% of height)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .padding(8.dp)
            ) {
                Text(
                    text = pdf.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FileUtils.formatFileSize(pdf.size),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
} 
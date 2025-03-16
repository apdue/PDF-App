package com.example.pdfjitpekc.screens

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pdfjitpekc.model.PdfFile
import com.example.pdfjitpekc.ui.PdfViewerScreen
import com.example.pdfjitpekc.ui.theme.PdfJitpekcTheme
import com.example.pdfjitpekc.util.FileUtils
import com.example.pdfjitpekc.util.showInterstitialAd
import com.example.pdfjitpekc.viewmodel.PdfViewModel
import java.io.File

class ReaderActivity : ComponentActivity() {
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

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                finish()
            }

            PdfJitpekcTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val selectedPdf by viewModel.selectedPdf.collectAsStateWithLifecycle(
                        initialValue = null
                    )

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
                                        finish()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
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
        permissionsGranted.value = hasPermission
        viewModel.updatePermissionStatus(hasPermission)

        if (hasPermission) {
            viewModel.loadPdfFiles()
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

    override fun onResume() {
        super.onResume()
        // Check permissions again when app comes back to foreground
        updatePermissionStatus()
    }
}

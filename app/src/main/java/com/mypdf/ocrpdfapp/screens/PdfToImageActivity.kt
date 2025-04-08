package com.mypdf.ocrpdfapp.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mypdf.ocrpdfapp.ui.theme.PDFTheme
import com.mypdf.ocrpdfapp.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfToImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get PDF path or URI from intent
        val pdfPath = intent.getStringExtra("pdf_path")
        val pdfUri = if (pdfPath != null) {
            Uri.fromFile(File(pdfPath))
        } else {
            intent.data ?: intent.getParcelableExtra("pdf_uri")
        }
        
        setContent {
            PDFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PdfToImageScreen(
                        initialPdfUri = pdfUri,
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToImageScreen(
    initialPdfUri: Uri?,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedPdfUri by remember { mutableStateOf<Uri?>(initialPdfUri) }
    var selectedPdfName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var conversionSuccess by remember { mutableStateOf(false) }
    var extractedImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var savedImagesPath by remember { mutableStateOf("") }
    var totalPages by remember { mutableStateOf(0) }

    // Load initial PDF if provided
    LaunchedEffect(initialPdfUri) {
        initialPdfUri?.let { uri ->
            selectedPdfUri = uri
            selectedPdfName = FileUtils.getFileName(context, uri) ?: "Selected PDF"
            // Reset state
            conversionSuccess = false
            extractedImages = emptyList()
            savedImagesPath = ""
        }
    }
    
    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedPdfUri = it
            selectedPdfName = FileUtils.getFileName(context, it) ?: "Selected PDF"
            // Reset state when new PDF is selected
            conversionSuccess = false
            extractedImages = emptyList()
            savedImagesPath = ""
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Convert PDF to Images") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selected PDF info
            if (selectedPdfUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Selected PDF",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = selectedPdfName,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        
                        if (totalPages > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalPages pages",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            
            // Select PDF button (only show if conversion not successful yet)
            if (!conversionSuccess) {
                Button(
                    onClick = { pickPdfLauncher.launch("application/pdf") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Select PDF"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select PDF File")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Convert button
                Button(
                    onClick = {
                        if (selectedPdfUri == null) {
                            Toast.makeText(context, "Please select a PDF file", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        coroutineScope.launch {
                            val result = convertPdfToImages(context, selectedPdfUri!!)
                            isLoading = false
                            
                            if (result.first) {
                                Toast.makeText(context, "Images extracted successfully", Toast.LENGTH_LONG).show()
                                // Set conversion success and store the extracted images
                                conversionSuccess = true
                                extractedImages = result.second
                                savedImagesPath = result.third
                                totalPages = extractedImages.size
                            } else {
                                Toast.makeText(context, "Failed to extract images", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = selectedPdfUri != null && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Converting...")
                    } else {
                        Text("Convert to Images")
                    }
                }
            }
            
            // Extracted images preview
            if (extractedImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Extracted Images (${extractedImages.size})",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(extractedImages) { bitmap ->
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(200.dp)
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Extracted image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Success message
                if (conversionSuccess) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Images Saved Successfully!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Saved to: $savedImagesPath",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Button to convert another PDF
                    Button(
                        onClick = {
                            // Reset state
                            selectedPdfUri = null
                            selectedPdfName = ""
                            conversionSuccess = false
                            extractedImages = emptyList()
                            savedImagesPath = ""
                            totalPages = 0
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Convert Another PDF")
                    }
                }
            }
        }
    }
}

suspend fun convertPdfToImages(context: Context, pdfUri: Uri): Triple<Boolean, List<Bitmap>, String> {
    return withContext(Dispatchers.IO) {
        try {
            val pdfRenderer = PdfRenderer(
                context.contentResolver.openFileDescriptor(pdfUri, "r")!!
            )
            
            val pageCount = pdfRenderer.pageCount
            val extractedImages = mutableListOf<Bitmap>()
            
            // Create directory if it doesn't exist
            val imagesDirectory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "PDFApp"
            )
            if (!imagesDirectory.exists()) {
                imagesDirectory.mkdirs()
            }
            
            // Create subfolder with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfName = FileUtils.getFileName(context, pdfUri)?.replace(".pdf", "") ?: "pdf"
            val outputDir = File(imagesDirectory, "${pdfName}_$timestamp")
            outputDir.mkdirs()
            
            // Extract each page as an image
            for (i in 0 until pageCount) {
                val page = pdfRenderer.openPage(i)
                
                // Create bitmap with the page dimensions
                val bitmap = Bitmap.createBitmap(
                    page.width,
                    page.height,
                    Bitmap.Config.ARGB_8888
                )
                
                // Render the page to the bitmap
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                // Save the bitmap to storage
                val imageFile = File(outputDir, "page_${i + 1}.jpg")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
                
                // Add to the list of extracted images
                extractedImages.add(bitmap)
                
                // Close the page
                page.close()
                
                // Add the image to the media store
                MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    imageFile.absolutePath,
                    imageFile.name,
                    "Extracted from PDF"
                )
            }
            
            // Close the renderer
            pdfRenderer.close()
            
            // Make the images visible in gallery
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(outputDir)
            context.sendBroadcast(intent)
            
            Triple(true, extractedImages, outputDir.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Triple(false, emptyList(), "")
        }
    }
} 
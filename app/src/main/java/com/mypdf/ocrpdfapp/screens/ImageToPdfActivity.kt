package com.mypdf.ocrpdfapp.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.mypdf.ocrpdfapp.ui.theme.PDFTheme
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ImageToPdfActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PDFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ImageToPdfScreen(
                        onBackPressed = { finish() },
                        onViewPdf = { pdfUri ->
                            val intent = Intent(this, ReaderActivity::class.java).apply {
                                data = pdfUri
                                action = Intent.ACTION_VIEW
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    onBackPressed: () -> Unit,
    onViewPdf: (Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var pdfFileName by remember { mutableStateOf("") }
    var createdPdfUri by remember { mutableStateOf<Uri?>(null) }
    var conversionSuccess by remember { mutableStateOf(false) }
    
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages = selectedImages + uris
            // Reset conversion state when new images are selected
            conversionSuccess = false
            createdPdfUri = null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Convert Images to PDF") },
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
            // File name input
            OutlinedTextField(
                value = pdfFileName,
                onValueChange = { pdfFileName = it },
                label = { Text("PDF File Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                enabled = !conversionSuccess
            )
            
            // Selected images preview
            if (selectedImages.isNotEmpty()) {
                Text(
                    text = "Selected Images (${selectedImages.size})",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    items(selectedImages) { uri ->
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(150.dp)
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Selected image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            if (!conversionSuccess) {
                                IconButton(
                                    onClick = {
                                        selectedImages = selectedImages.filter { it != uri }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(50))
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove image",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add images button (only show if conversion not successful yet)
            if (!conversionSuccess) {
                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add images"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Images")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Convert button
                Button(
                    onClick = {
                        if (selectedImages.isEmpty()) {
                            Toast.makeText(context, "Please select at least one image", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        if (pdfFileName.isBlank()) {
                            Toast.makeText(context, "Please enter a file name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        coroutineScope.launch {
                            val result = convertImagesToPdf(context, selectedImages, pdfFileName)
                            isLoading = false
                            
                            if (result.first) {
                                Toast.makeText(context, "PDF created successfully", Toast.LENGTH_LONG).show()
                                // Set conversion success and store the PDF URI
                                conversionSuccess = true
                                createdPdfUri = result.second
                            } else {
                                Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = selectedImages.isNotEmpty() && pdfFileName.isNotBlank() && !isLoading,
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
                        Text("Convert to PDF")
                    }
                }
            } else {
                // Show success message and View PDF button
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
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "PDF Created Successfully!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "File: $pdfFileName",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { 
                                createdPdfUri?.let { onViewPdf(it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = createdPdfUri != null
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "View PDF"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View PDF")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Button to create another PDF
                OutlinedButton(
                    onClick = {
                        // Reset state
                        selectedImages = emptyList()
                        pdfFileName = ""
                        conversionSuccess = false
                        createdPdfUri = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Convert Another PDF")
                }
            }
        }
    }
}

suspend fun convertImagesToPdf(context: Context, imageUris: List<Uri>, fileName: String): Pair<Boolean, Uri?> {
    return withContext(Dispatchers.IO) {
        try {
            // Create PDF document
            val document = Document(PageSize.A4)
            
            // Create directory if it doesn't exist
            val pdfDirectory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "PDFApp"
            )
            if (!pdfDirectory.exists()) {
                pdfDirectory.mkdirs()
            }
            
            // Create file with timestamp to avoid overwriting
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val finalFileName = if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf"
            val pdfFile = File(pdfDirectory, finalFileName)
            
            // Initialize PDF writer
            val pdfWriter = PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()
            
            // Add each image to the PDF
            for (uri in imageUris) {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                
                val imageBytes = stream.toByteArray()
                val image = Image.getInstance(imageBytes)
                
                // Scale image to fit page
                val pageWidth = document.pageSize.width - document.leftMargin() - document.rightMargin()
                val pageHeight = document.pageSize.height - document.topMargin() - document.bottomMargin()
                
                if (image.width > pageWidth || image.height > pageHeight) {
                    image.scaleToFit(pageWidth, pageHeight)
                }
                
                // Center image on page
                image.setAbsolutePosition(
                    (document.pageSize.width - image.scaledWidth) / 2,
                    (document.pageSize.height - image.scaledHeight) / 2
                )
                
                document.add(image)
                document.newPage()
            }
            
            document.close()
            pdfWriter.close()
            
            // Make the PDF visible in gallery/files app
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val uri = Uri.fromFile(pdfFile)
            intent.data = uri
            context.sendBroadcast(intent)
            
            Pair(true, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, null)
        }
    }
} 
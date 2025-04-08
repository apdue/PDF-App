package com.mypdf.ocrpdfapp.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.mypdf.ocrpdfapp.R
import com.mypdf.ocrpdfapp.ui.theme.PDFTheme
import com.mypdf.ocrpdfapp.util.FileUtils
import com.github.gcacace.signaturepad.views.SignaturePad
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SignPdfActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize PDFBox
        PDFBoxResourceLoader.init(applicationContext)
        
        // Get PDF path or URI from intent
        val pdfPath = intent.getStringExtra("pdf_path")
        val pdfUri = if (pdfPath != null) {
            Uri.fromFile(File(pdfPath))
        } else {
            intent.getParcelableExtra("pdf_uri")
        }
        
        setContent {
            PDFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SignPdfScreen(
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
fun SignPdfScreen(
    initialPdfUri: Uri?,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedPdfUri by remember { mutableStateOf<Uri?>(initialPdfUri) }
    var selectedPdfName by remember { mutableStateOf("") }
    var pdfPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var signatureComplete by remember { mutableStateOf(false) }
    var signedPdfPath by remember { mutableStateOf("") }
    var totalPages by remember { mutableStateOf(0) }
    var currentPage by remember { mutableStateOf(0) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var signaturePosition by remember { mutableStateOf(Offset(0f, 0f)) }
    var isSignaturePlaced by remember { mutableStateOf(false) }
    var isSignatureMode by remember { mutableStateOf(false) }

    // Load initial PDF if provided
    LaunchedEffect(initialPdfUri) {
        initialPdfUri?.let { uri ->
            selectedPdfUri = uri
            selectedPdfName = FileUtils.getFileName(context, uri) ?: "Selected PDF"
            // Reset state
            signatureComplete = false
            signedPdfPath = ""
            isSignaturePlaced = false
            isSignatureMode = false

            // Load PDF preview
            coroutineScope.launch {
                val result = loadPdfPreview(context, uri)
                pdfPreviewBitmap = result.first
                totalPages = result.second
                currentPage = 0
            }
        }
    }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedPdfUri = it
            selectedPdfName = FileUtils.getFileName(context, it) ?: "Selected PDF"
            // Reset state when new PDF is selected
            signatureComplete = false
            signedPdfPath = ""
            isSignaturePlaced = false
            isSignatureMode = false

            // Load PDF preview
            coroutineScope.launch {
                val result = loadPdfPreview(context, it)
                pdfPreviewBitmap = result.first
                totalPages = result.second
                currentPage = 0
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign PDF") },
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
                                text = "Page ${currentPage + 1} of $totalPages",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // PDF Preview
            if (pdfPreviewBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .border(
                            1.dp,
                            androidx.compose.ui.graphics.Color.Gray,
                            RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(androidx.compose.ui.graphics.Color.White)
                        .pointerInput(Unit) {
                            if (isSignatureMode && signatureBitmap != null) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()

                                    // Update position with bounds checking using fixed dimensions
                                    signaturePosition = Offset(
                                        (signaturePosition.x + dragAmount.x).coerceIn(
                                            0f,
                                            size.width - 200f
                                        ),
                                        (signaturePosition.y + dragAmount.y).coerceIn(
                                            0f,
                                            size.height - 100f
                                        )
                                    )
                                    isSignaturePlaced = true
                                }
                            }
                        }
                ) {
                    Image(
                        bitmap = pdfPreviewBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Display signature on top of PDF if in signature mode
                    if (isSignatureMode && signatureBitmap != null) {
                        // Add a semi-transparent overlay to indicate signature mode
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f))
                        )

                        // Show signature with a border to make it more visible
                        Box(
                            modifier = Modifier
                                .size(200.dp, 100.dp)
                                .offset(
                                    x = with(LocalDensity.current) { signaturePosition.x.toDp() },
                                    y = with(LocalDensity.current) { signaturePosition.y.toDp() }
                                )
                                .border(
                                    width = 1.dp,
                                    color = androidx.compose.ui.graphics.Color.Blue.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        ) {
                            Image(
                                bitmap = signatureBitmap!!.asImageBitmap(),
                                contentDescription = "Signature",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // Add instructions text
                        Text(
                            text = "Drag signature to position, then tap 'Apply Signature'",
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Page navigation
                if (totalPages > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentPage > 0) {
                                    currentPage--
                                    coroutineScope.launch {
                                        pdfPreviewBitmap =
                                            loadPdfPage(context, selectedPdfUri!!, currentPage)
                                    }
                                }
                            },
                            enabled = currentPage > 0
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Previous Page")
                        }

                        Text(
                            text = "Page ${currentPage + 1} of $totalPages",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        IconButton(
                            onClick = {
                                if (currentPage < totalPages - 1) {
                                    currentPage++
                                    coroutineScope.launch {
                                        pdfPreviewBitmap =
                                            loadPdfPage(context, selectedPdfUri!!, currentPage)
                                    }
                                }
                            },
                            enabled = currentPage < totalPages - 1
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next Page")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            if (!signatureComplete) {
                // Select PDF button (if no PDF selected)
                if (selectedPdfUri == null) {
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
                } else {
                    // Signature actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Create signature button
                        Button(
                            onClick = { showSignatureDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = "Create Signature"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Signature")
                        }

                        // Place signature button (only enabled if signature exists)
                        Button(
                            onClick = {
                                if (!isSignatureMode) {
                                    // Entering signature mode - center the signature on the PDF
                                    // Get the dimensions of the PDF preview container
                                    val previewContainerWidth = 400f
                                    val previewContainerHeight = 400f

                                    // Center the signature on the PDF
                                    signaturePosition = Offset(
                                        previewContainerWidth / 2 - 100f, // Center horizontally (200px width / 2)
                                        previewContainerHeight / 2 - 50f  // Center vertically (100px height / 2)
                                    )
                                    isSignatureMode = true
                                    isSignaturePlaced = true // Consider it placed immediately
                                } else {
                                    // Apply signature to PDF
                                    isLoading = true
                                    coroutineScope.launch {
                                        val result = applySignatureToPdf(
                                            context,
                                            selectedPdfUri!!,
                                            signatureBitmap!!,
                                            signaturePosition,
                                            currentPage
                                        )
                                        isLoading = false

                                        if (result.first) {
                                            Toast.makeText(
                                                context,
                                                "PDF signed successfully",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            signatureComplete = true
                                            signedPdfPath = result.second
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to sign PDF: ${result.second}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                    isSignatureMode = false
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            enabled = !isLoading && signatureBitmap != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSignatureMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = if (isSignatureMode) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isSignatureMode) "Apply Signature" else "Place Signature"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSignatureMode) "Apply Signature" else "Place Signature")
                        }
                    }
                }
            } else {
                // Success message
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
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "PDF Signed Successfully!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Saved to: $signedPdfPath",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // View signed PDF button
                        Button(
                            onClick = {
                                val intent = Intent(context, ReaderActivity::class.java).apply {
                                    data = Uri.fromFile(File(signedPdfPath))
                                    action = Intent.ACTION_VIEW
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "View PDF"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Signed PDF")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Button to sign another PDF
                Button(
                    onClick = {
                        // Reset state
                        selectedPdfUri = null
                        selectedPdfName = ""
                        pdfPreviewBitmap = null
                        signatureComplete = false
                        signedPdfPath = ""
                        totalPages = 0
                        currentPage = 0
                        signatureBitmap = null
                        isSignaturePlaced = false
                        isSignatureMode = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Another PDF")
                }
            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Signature Dialog with SignaturePad library
    if (showSignatureDialog) {
        SignaturePadDialog(
            onDismiss = { showSignatureDialog = false },
            onSignatureCreated = { bitmap ->
                signatureBitmap = bitmap
                showSignatureDialog = false
            }
        )
    }
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSignatureCreated: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Your Signature",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SignaturePad from the library
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(
                            1.dp,
                            androidx.compose.ui.graphics.Color.Gray,
                            RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp)),
                    factory = { ctx ->
                        // Inflate the SignaturePad view
                        val signaturePad = SignaturePad(ctx, null).apply {
                            setBackgroundColor(Color.WHITE)
                            setPenColor(Color.BLACK)
                            setMinWidth(8f)
                            setMaxWidth(16f)
                        }
                        
                        // Create a container view to hold the SignaturePad and buttons
                        val container = LayoutInflater.from(ctx).inflate(
                            R.layout.signature_pad_layout,
                            null, 
                            false
                        )
                        
                        // Add the SignaturePad to the container
                        val signaturePadContainer = container.findViewById<View>(R.id.signature_pad_container)
                        (signaturePadContainer as? android.widget.FrameLayout)?.addView(signaturePad)
                        
                        // Set up the clear button
                        container.findViewById<Button>(R.id.clear_button).setOnClickListener {
                            signaturePad.clear()
                        }
                        
                        // Set up the save button
                        container.findViewById<Button>(R.id.save_button).setOnClickListener {
                            if (!signaturePad.isEmpty) {
                                val signatureBitmap = signaturePad.signatureBitmap
                                onSignatureCreated(signatureBitmap)
                            } else {
                                Toast.makeText(ctx, "Please sign before saving", Toast.LENGTH_SHORT).show()
                            }
                        }
                        
                        container
                    }
                )
            }
        }
    }
}

suspend fun loadPdfPreview(context: Context, pdfUri: Uri): Pair<Bitmap, Int> {
    return withContext(Dispatchers.IO) {
        try {
            val pdfRenderer = PdfRenderer(
                context.contentResolver.openFileDescriptor(pdfUri, "r")!!
            )

            val pageCount = pdfRenderer.pageCount
            val firstPage = pdfRenderer.openPage(0)

            val bitmap = Bitmap.createBitmap(
                firstPage.width,
                firstPage.height,
                Bitmap.Config.ARGB_8888
            )

            firstPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            firstPage.close()
            pdfRenderer.close()

            Pair(bitmap, pageCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), 0)
        }
    }
}

suspend fun loadPdfPage(context: Context, pdfUri: Uri, pageIndex: Int): Bitmap {
    return withContext(Dispatchers.IO) {
        try {
            val pdfRenderer = PdfRenderer(
                context.contentResolver.openFileDescriptor(pdfUri, "r")!!
            )

            val page = pdfRenderer.openPage(pageIndex)

            val bitmap = Bitmap.createBitmap(
                page.width,
                page.height,
                Bitmap.Config.ARGB_8888
            )

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            pdfRenderer.close()

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }
}

suspend fun applySignatureToPdf(
    context: Context,
    pdfUri: Uri,
    signatureBitmap: Bitmap,
    position: Offset,
    pageIndex: Int
): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            // Initialize PDFBox
            PDFBoxResourceLoader.init(context.applicationContext)

            // Create directory if it doesn't exist
            val pdfDirectory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "PDFApp/Signed"
            )
            if (!pdfDirectory.exists()) {
                pdfDirectory.mkdirs()
            }

            // Create output file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfName = FileUtils.getFileName(context, pdfUri)?.replace(".pdf", "") ?: "document"
            val outputFile = File(pdfDirectory, "${pdfName}_signed_$timestamp.pdf")

            // Get input stream from URI
            val inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: return@withContext Pair(false, "Could not open PDF file")

            val tempFile = File.createTempFile("temp_pdf", ".pdf", context.cacheDir)
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            // Load the PDF document
            val document = PDDocument.load(tempFile)

            if (pageIndex >= document.numberOfPages) {
                document.close()
                return@withContext Pair(false, "Invalid page index: $pageIndex")
            }

            // Get the page to add the signature to
            val page = document.getPage(pageIndex)
            val pageWidth = page.mediaBox.width
            val pageHeight = page.mediaBox.height

            // Create PDImageXObject from the signature bitmap
            val signatureImage = LosslessFactory.createFromImage(document, signatureBitmap)

            // Scale the signature to a reasonable size (15% of page width)
            val targetWidth = pageWidth * 0.15f
            val scale = targetWidth / signatureImage.width
            val scaledWidth = signatureImage.width * scale
            val scaledHeight = signatureImage.height * scale

            // Calculate the position in PDF coordinates
            // The preview container is 400x400, so we need to scale the position
            val scaleFactorX = pageWidth / 400f
            val scaleFactorY = pageHeight / 400f

            // Convert position from preview coordinates to PDF coordinates
            // PDF coordinates start from bottom-left, UI coordinates from top-left
            val pdfX = position.x * scaleFactorX
            val pdfY = pageHeight - (position.y * scaleFactorY) - scaledHeight

            // Create a content stream to draw on the page
            val contentStream = PDPageContentStream(
                document,
                page,
                PDPageContentStream.AppendMode.APPEND,
                true,
                true
            )

            // Draw the signature image at the calculated position
            contentStream.drawImage(
                signatureImage,
                pdfX,
                pdfY,
                scaledWidth,
                scaledHeight
            )

            // Close the content stream
            contentStream.close()

            // Save the document to the output file
            document.save(outputFile)
            document.close()
            tempFile.delete()

            // Make the PDF visible in files app
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(outputFile)
            context.sendBroadcast(intent)

            Pair(true, outputFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, e.message ?: "Unknown error")
        }
    }
} 
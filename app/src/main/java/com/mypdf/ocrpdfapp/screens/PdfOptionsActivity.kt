package com.mypdf.ocrpdfapp.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.mypdf.ocrpdfapp.signer.DigitalSignatureActivity
import com.mypdf.ocrpdfapp.ui.PdfCompressActivity
import com.mypdf.ocrpdfapp.ui.PdfSplitActivity
import com.mypdf.ocrpdfapp.ui.PdfTextExtractActivity
import com.mypdf.ocrpdfapp.ui.icons.CustomIcons
import com.mypdf.ocrpdfapp.ui.theme.PDFTheme
import com.mypdf.ocrpdfapp.util.ComposeAdsManager
import com.mypdf.ocrpdfapp.util.FileUtils
import com.mypdf.ocrpdfapp.util.PrefManagerVideo
import java.io.File
import java.io.FileOutputStream

class PdfOptionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the PDF URI from the intent
        val pdfUri = intent.data ?: intent.getParcelableExtra<Uri>("pdf_uri")
        val pdfPath = intent.getStringExtra("pdf_path")

        if (pdfUri == null && pdfPath == null) {
            finish()
            return
        }

        setContent {
            PDFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PdfOptionsScreen(
                        pdfUri = pdfUri,
                        pdfPath = pdfPath,
                        pref = PrefManagerVideo(this@PdfOptionsActivity),
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfOptionsScreen(
    pdfUri: Uri?,
    pdfPath: String?,
    pref: PrefManagerVideo,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Get the file name from the URI or path
    val fileName = remember {
        when {
            pdfUri != null -> FileUtils.getFileName(context, pdfUri) ?: "PDF Document"
            pdfPath != null -> pdfPath.substringAfterLast("/")
            else -> "PDF Document"
        }
    }

    var showAd by remember { mutableStateOf(false) }
    var onAdClosedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Options") },
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

            ComposeAdsManager.NativeAd()

            // PDF File Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp, bottom = 15.dp),
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
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Options Section Title
            Text(
                text = "What would you like to do?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Options Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: Open and Sign
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (pref.getString(SplashActivity.open_button).contains("true")) {

                        // Open PDF Option
                        PdfOptionCard(
                            icon = Icons.Default.OpenInNew,
                            title = "Open",
                            description = "View the PDF document",
                            modifier = Modifier.weight(1f),
                            onClick = {

                                val intent = Intent(context, ReaderActivity::class.java).apply {
                                    if (pdfUri != null) {
                                        data = pdfUri
                                    } else if (pdfPath != null) {
                                        data = Uri.fromFile(File(pdfPath))
                                    }
                                    action = Intent.ACTION_VIEW
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }

                                showInterstitialAd {
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }


                    if (pref.getString(SplashActivity.sign_button).contains("true")) {

                        // Sign PDF Option
                        PdfOptionCard(
                            icon = Icons.Default.Create,
                            title = "Sign",
                            description = "Add your signature to the document",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val intent =
                                    Intent(context, DigitalSignatureActivity::class.java).apply {
                                        putExtra("ActivityAction", "PDFOpen")
                                        val uris = ArrayList<Uri>()
                                        when {
                                            pdfUri != null -> uris.add(pdfUri)
                                            pdfPath != null -> uris.add(Uri.fromFile(File(pdfPath)))
                                        }
                                        putParcelableArrayListExtra("PDFOpen", uris)
                                    }
                                showInterstitialAd {
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }


                }

                // Row 2: Convert to Image and Extract Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {


                    if (pref.getString(SplashActivity.convert_to_images_button).contains("true")) {

                        // Convert to Image Option
                        PdfOptionCard(
                            icon = Icons.Default.Image,
                            title = "Convert to Images",
                            description = "Extract pages as images",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val intent = Intent(context, PdfToImageActivity::class.java).apply {
                                    if (pdfPath != null) {
                                        putExtra("pdf_path", pdfPath)
                                    } else if (pdfUri != null) {
                                        putExtra("pdf_uri", pdfUri)
                                    }
                                }
                                showInterstitialAd {
                                    context.startActivity(intent)
                                }
                            }
                        )

                    }

                    if (pref.getString(SplashActivity.extract_text_button).contains("true")) {

                        // Extract Text Option
                        PdfOptionCard(
                            icon = Icons.Default.TextFields,
                            title = "Extract Text",
                            description = "Extract text from PDF",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // Launch PdfTextExtractActivity
                                val intent =
                                    Intent(context, PdfTextExtractActivity::class.java).apply {
                                        putExtra("pdf_path", pdfPath)
                                    }
                                showInterstitialAd {
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }


                }

                // Row 3: Share and Print
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    if (pref.getString(SplashActivity.share_button).contains("true")) {

                        // Share Option
                        PdfOptionCard(
                            icon = Icons.Default.Share,
                            title = "Share",
                            description = "Share the document",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                try {
                                    val uri = when {
                                        pdfUri != null -> pdfUri
                                        pdfPath != null -> FileProvider.getUriForFile(
                                            context,
                                            "${context.applicationContext.packageName}.provider",
                                            File(pdfPath)
                                        )

                                        else -> null
                                    }

                                    if (uri == null) {
                                        Toast.makeText(
                                            context,
                                            "Error: Cannot share PDF",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@PdfOptionCard
                                    }

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            "Share PDF"
                                        )
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Error sharing PDF: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }

                    if (pref.getString(SplashActivity.print_button).contains("true")) {
                        // Print Option
                        PdfOptionCard(
                            icon = Icons.Default.Print,
                            title = "Print",
                            description = "Print the document",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                try {
                                    val printManager =
                                        context.getSystemService(ComponentActivity.PRINT_SERVICE) as PrintManager
                                    val jobName = "Document_${File(pdfPath).name}"

                                    val pdfFile = File(pdfPath)
                                    val uri = Uri.fromFile(pdfFile)

                                    // Create a print adapter that uses the PDF content provider
                                    val printAdapter = object : PrintDocumentAdapter() {
                                        override fun onLayout(
                                            oldAttributes: PrintAttributes?,
                                            newAttributes: PrintAttributes?,
                                            cancellationSignal: CancellationSignal?,
                                            callback: LayoutResultCallback?,
                                            extras: Bundle?
                                        ) {
                                            if (cancellationSignal?.isCanceled == true) {
                                                callback?.onLayoutCancelled()
                                                return
                                            }

                                            val info = PrintDocumentInfo.Builder(pdfFile.name)
                                                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                                                .build()

                                            callback?.onLayoutFinished(
                                                info,
                                                oldAttributes != newAttributes
                                            )
                                        }

                                        override fun onWrite(
                                            pages: Array<out PageRange>?,
                                            destination: ParcelFileDescriptor?,
                                            cancellationSignal: CancellationSignal?,
                                            callback: WriteResultCallback?
                                        ) {
                                            try {
                                                val input =
                                                    context.contentResolver.openInputStream(uri)
                                                val output =
                                                    FileOutputStream(destination?.fileDescriptor)

                                                val buffer = ByteArray(1024)
                                                var bytesRead: Int

                                                while (input?.read(buffer)
                                                        .also { bytesRead = it ?: -1 } != -1
                                                ) {
                                                    if (cancellationSignal?.isCanceled == true) {
                                                        callback?.onWriteCancelled()
                                                        input?.close()
                                                        output.close()
                                                        return
                                                    }
                                                    output.write(buffer, 0, bytesRead)
                                                }

                                                input?.close()
                                                output.close()
                                                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))

                                            } catch (e: Exception) {
                                                callback?.onWriteFailed(e.message)
                                            }
                                        }
                                    }

                                    printManager.print(jobName, printAdapter, null)
                                    Toast.makeText(
                                        context,
                                        "Preparing document for printing...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )

                    }

                }

                // Row 4: Edit and Compress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    if (pref.getString(SplashActivity.edit_button).contains("true")) {

                        // Split Option
                        PdfOptionCard(
                            icon = Icons.Default.ContentCut,
                            title = "Edit Pages",
                            description = "Split or delete pages",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // Launch PdfSplitActivity
                                val intent = Intent(context, PdfSplitActivity::class.java).apply {
                                    putExtra("pdf_path", pdfPath)
                                }
                                showInterstitialAd {
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }

                    if(pref.getString(SplashActivity.compress_button).contains("true")) {
                        // Compress Option
                        PdfOptionCard(
                            icon = CustomIcons.Compress,
                            title = "Compress",
                            description = "Reduce file size",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // Launch PdfCompressActivity
                                val intent = Intent(context, PdfCompressActivity::class.java).apply {
                                    putExtra("pdf_path", pdfPath)
                                }
                                showInterstitialAd {
                                    context.startActivity(intent)
                                }
                            }
                        )

                    }


                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(140.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
} 
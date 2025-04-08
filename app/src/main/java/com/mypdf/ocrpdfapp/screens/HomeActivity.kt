package com.mypdf.ocrpdfapp.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.mypdf.ocrpdfapp.R
import com.mypdf.ocrpdfapp.dummy.FifthActivity
import com.mypdf.ocrpdfapp.dummy.FirstActivity
import com.mypdf.ocrpdfapp.dummy.FourthActivity
import com.mypdf.ocrpdfapp.dummy.SecondActivity
import com.mypdf.ocrpdfapp.dummy.SixthActivity
import com.mypdf.ocrpdfapp.dummy.ThirdActivity
import com.mypdf.ocrpdfapp.signer.DigitalSignatureActivity
import com.mypdf.ocrpdfapp.ui.theme.PDFTheme
import com.mypdf.ocrpdfapp.util.AdsManager
import com.mypdf.ocrpdfapp.util.ComposeAdsManager
import com.mypdf.ocrpdfapp.util.PrefManagerVideo

class HomeActivity : ComponentActivity() {

    private var showExitDialog = mutableStateOf(false)

    lateinit var pref: PrefManagerVideo
    lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = PrefManagerVideo(this)
        analytics = FirebaseAnalytics.getInstance(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
        setContent {
            // Handle back press
            BackHandler(enabled = true) {

                val intent: Intent
                if (PrefManagerVideo(this).getString(SplashActivity.status_dummy_six_back_enabled)
                        .contains("true")
                ) {
                    intent = Intent(this, SixthActivity::class.java)
                    AdsManager.showInterstitialAd(
                        this
                    ) { startActivity(intent) }
                } else if (PrefManagerVideo(this).getString(SplashActivity.status_dummy_five_back_enabled)
                        .contains("true") && !PrefManagerVideo(this).getString(SplashActivity.TAG_NATIVEID)
                    .contains("sandeep")
                ) {
                    intent = Intent(this, FifthActivity::class.java)
                    AdsManager.showInterstitialAd(
                        this
                    ) { startActivity(intent) }
                } else if (PrefManagerVideo(this).getString(SplashActivity.status_dummy_four_back_enabled)
                        .contains("true")
                ) {
                    intent = Intent(this, FourthActivity::class.java)
                    AdsManager.showInterstitialAd(
                        this
                    ) { startActivity(intent) }
                } else if (PrefManagerVideo(this).getString(SplashActivity.status_dummy_three_back_enabled)
                        .contains("true")
                ) {
                    intent = Intent(this, ThirdActivity::class.java)

                    AdsManager.showInterstitialAd(
                        this
                    ) { startActivity(intent) }
                } else if (PrefManagerVideo(this).getString(SplashActivity.status_dummy_two_back_enabled)
                        .contains("true")
                ) {
                    intent = Intent(this, SecondActivity::class.java)

                    AdsManager.showInterstitialAd(
                        this
                    ) { startActivity(intent) }
                } else if (PrefManagerVideo(this).getString(SplashActivity.status_dummy_one_back_enabled)
                        .contains("true")
                ) {
                    intent = Intent(this, FirstActivity::class.java)
                    AdsManager.showInterstitialAd(
                        this
                    ) { startActivity(intent) }
                } else {
                    showExitDialog.value = true
                }
            }

            PDFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HomeScreen(
                            onPdfReaderClick = {
                                startActivity(Intent(this@HomeActivity, MainActivity::class.java))
                            },
                            onConvertImagesClick = {
                                startActivity(
                                    Intent(
                                        this@HomeActivity,
                                        ImageToPdfActivity::class.java
                                    )
                                )
                            },
                            onPdfToImagesClick = {
                                startActivity(
                                    Intent(
                                        this@HomeActivity,
                                        PdfToImageActivity::class.java
                                    )
                                )
                            },
                            onPPClick = {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setData(Uri.parse("https://sites.google.com/view/privacypolicypdfsmal/"))
                                startActivity(intent)
                            },
                            onSignPdfClick = {
                                val intent = Intent(
                                    this@HomeActivity,
                                    DigitalSignatureActivity::class.java
                                ).apply {
                                    putExtra("ActivityAction", "FileSearch")
                                }
                                startActivity(intent)
                            }
                        )

                        // Exit Confirmation Dialog
                        if (showExitDialog.value) {
                            ExitConfirmationDialog(
                                onDismiss = { showExitDialog.value = false },
                                onConfirm = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPdfReaderClick: () -> Unit,
    onConvertImagesClick: () -> Unit,
    onPPClick: () -> Unit,
    onPdfToImagesClick: () -> Unit,
    onSignPdfClick: () -> Unit
) {
    var showAd by remember { mutableStateOf(false) }
    var onAdClosedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current

    val pref = PrefManagerVideo(context)

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
                title = { Text("PDF Viewer") },
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
                .padding(top = 25.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            var scale by remember { mutableStateOf(0f) }

            LaunchedEffect(Unit) {
                animate(
                    initialValue = 0.8f,
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) { value, _ ->
                    scale = value
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                ComposeAdsManager.NativeAd(adSize = 2)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (pref.getString(SplashActivity.pdf_editor_button)
                    .contains("true", ignoreCase = false)
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        showInterstitialAd {
                            onPdfReaderClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = "PDFs",
                        fontSize = 16.sp
                    )
                }

            }

            if (pref.getString(SplashActivity.image_to_pdf_button)
                    .contains("true", ignoreCase = false)
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                // Convert Images to PDF Button
                Button(
                    onClick = {
                        showInterstitialAd {
                            onConvertImagesClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "Convert Images to PDF",
                        fontSize = 18.sp
                    )
                }

            }


            if (pref.getString(SplashActivity.pdf_to_image_button)
                    .contains("true", ignoreCase = false)
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                // Convert PDF to Images Button
                Button(
                    onClick = {
                        showInterstitialAd {
                            onPdfToImagesClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(
                        text = "Convert PDF to Images",
                        fontSize = 18.sp
                    )
                }

            }


            if (pref.getString(SplashActivity.sign_pdf_button)
                    .contains("true", ignoreCase = false)
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                // Convert PDF to Images Button
                Button(
                    onClick = {
                        showInterstitialAd {
                            onSignPdfClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Sign PDF",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Privacy Policy",
                    fontSize = 18.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        onPPClick()
                    }
                )
            }

            Image(
                painter = painterResource(R.drawable.img_pdf_3),
                contentDescription = "pdf-image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(15.dp)
            )

            Text(
                text = "Powerful PDF Reader",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 15.dp)
            )

            Text(
                text = "Keep your important files safe with reliable processing and document integrity.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 15.dp)
            )
        }
    }
}

@Composable
fun ExitConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var scale by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { value, _ ->
            scale = value
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .scale(scale),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Exit Application?",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Are you sure you want to exit the application?",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exit")
                    }
                }
            }
        }
    }
} 
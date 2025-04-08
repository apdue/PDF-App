package com.mypdf.ocrpdfapp.util

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Utility class for displaying ads in Jetpack Compose
 * This class uses the existing AdsManager functionality and wraps it in Compose components
 */
object ComposeAdsManager {
    
    /**
     * Composable function to display a native ad
     * @param modifier Modifier for the ad container
     * @param adSize 0 for small, 1 for large, 2 for medium
     */
    @Composable
    fun NativeAd(
        modifier: Modifier = Modifier,
        adSize: Int = 1
    ) {
        val context = LocalContext.current
        val activity = remember { context as Activity }
        
        Box(modifier = modifier.fillMaxWidth()) {
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                },
                update = { frameLayout ->
                    AdsManager.showAndLoadNativeAd(activity, frameLayout, adSize)
                }
            )
        }
    }
    
    /**
     * Composable function to show an interstitial ad with a callback
     * @param onAdFinished Callback to be invoked when the ad is finished
     */
    @Composable
    fun InterstitialAd(
        onAdFinished: () -> Unit
    ) {
        val context = LocalContext.current
        val activity = remember { context as Activity }
        
        DisposableEffect(Unit) {
            AdsManager.showInterstitialAd(activity) {
                onAdFinished()
            }
            onDispose { }
        }
    }
    
    /**
     * Composable function for a small native ad
     */
    @Composable
    fun SmallNativeAd(
        modifier: Modifier = Modifier
    ) {
        NativeAd(
            modifier = modifier.height(100.dp),
            adSize = 0
        )
    }
    
    /**
     * Composable function for a medium native ad
     */
    @Composable
    fun MediumNativeAd(
        modifier: Modifier = Modifier
    ) {
        NativeAd(
            modifier = modifier.height(250.dp),
            adSize = 2
        )
    }
    
    /**
     * Composable function for a large native ad
     */
    @Composable
    fun LargeNativeAd(
        modifier: Modifier = Modifier
    ) {
        NativeAd(
            modifier = modifier.height(350.dp),
            adSize = 1
        )
    }
    
    /**
     * Initialize AdMob for the activity
     * Should be called in your main activity's onCreate
     */
    fun initialize(activity: Activity) {
        AdsManager.initializeAdMob(activity)
    }
    
    /**
     * Load an interstitial ad in advance
     * Call this method to preload the next interstitial ad
     */
    fun preloadInterstitialAd(activity: Activity) {
        AdsManager.loadInterstitialAd(activity)
    }
}

// Extension function to make it easier to show interstitial ads
fun Activity.showInterstitialAd(onAdFinished: () -> Unit) {
    AdsManager.showInterstitialAd(this, object : AdsManager.AdFinished {
        override fun onAdFinished() {
            onAdFinished()
        }
    })
} 
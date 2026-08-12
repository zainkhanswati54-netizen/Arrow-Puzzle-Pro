package com.arrowpuzzle.game.core.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Anchored banner ("Banner-arrow" unit). Drop this at the bottom of a screen,
 * outside of any scroll container, same as a native bottom bar.
 */
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdIds.BANNER
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

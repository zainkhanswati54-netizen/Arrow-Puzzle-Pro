package com.arrowpuzzle.game.core.ads

import com.arrowpuzzle.game.BuildConfig

/**
 * Every AdMob identifier the app uses, in one place. Debug builds always use
 * Google's official test IDs so development/CI never accidentally serves (or
 * clicks) real ads — release builds use the live IDs below.
 */
object AdIds {

    const val APP_ID = "ca-app-pub-9019700052213764~2171611579"

    val BANNER: String
        get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111"
        else "ca-app-pub-9019700052213764/3749889300"

    val INTERSTITIAL: String
        get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712"
        else "ca-app-pub-9019700052213764/3725838572"

    // ⚠️ The unit ID sent for "Rewarded interstitial – Hint" was the App ID
    // repeated twice, not an actual ad-unit ID (those look like
    // ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN). Falling back to Google's
    // public test ID so the build compiles and hint-ads work in the
    // meantime — swap this for the real ad-unit ID from AdMob once you have
    // it, or this will keep serving test ads in production.
    val REWARDED_INTERSTITIAL_HINT: String
        get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5354046379"
        else "ca-app-pub-3940256099942544/5354046379" // TODO: replace with real unit ID

    val REWARDED_ARROW: String
        get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917"
        else "ca-app-pub-9019700052213764/8810644297"
}

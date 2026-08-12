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

    // ⚠️ ACTION NEEDED — this is the one ad unit still on Google's public
    // test ID in release builds. Since hints are now ad-only (no free
    // hints — see PuzzleState.hintsRemaining), this is the ad real players
    // will hit constantly, so it needs a real unit before shipping:
    //   1. AdMob console → Apps → Arrow Puzzle → Ad units
    //   2. Create (or find) the "Rewarded interstitial" unit for Hint
    //   3. Replace the release-branch string below with it — it will look
    //      like ca-app-pub-9019700052213764/NNNNNNNNNN (same app ID as the
    //      other units above, different 10-digit suffix).
    val REWARDED_INTERSTITIAL_HINT: String
        get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5354046379"
        else "ca-app-pub-3940256099942544/5354046379" // TODO: replace with real unit ID before release

    val REWARDED_ARROW: String
        get() = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917"
        else "ca-app-pub-9019700052213764/8810644297"
}

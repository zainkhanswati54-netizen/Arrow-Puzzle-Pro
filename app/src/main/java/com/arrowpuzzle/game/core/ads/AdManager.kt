package com.arrowpuzzle.game.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

private const val TAG = "AdManager"

/**
 * Single owner of every ad unit's lifecycle: consent, loading, caching, and
 * showing. Screens never touch the AdMob SDK directly — they call into this
 * object and get a simple success/skip callback back, so a slow or failed ad
 * load never blocks gameplay.
 */
object AdManager {

    private var initialized = false
    private lateinit var consentInformation: ConsentInformation

    private var interstitialAd: InterstitialAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    // Simple frequency cap so the interstitial never shows on the very first
    // couple of levels and then only every Nth completion after that —
    // enough presence to monetize without souring the early experience.
    private const val LEVELS_BEFORE_FIRST_INTERSTITIAL = 3
    private const val LEVELS_BETWEEN_INTERSTITIALS = 3
    private var levelsSinceLastInterstitial = 0

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        consentInformation = UserMessagingPlatform.getConsentInformation(context)
        MobileAds.initialize(context) { Log.d(TAG, "MobileAds initialized") }
        preloadAll(context)
    }

    /**
     * Runs the GDPR/UMP consent flow (only actually shows a form to users in
     * regions where it's required — everyone else is a no-op). Call once,
     * from the first Activity, before requesting ads.
     */
    fun requestConsent(activity: Activity, onComplete: () -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .apply {
                if (com.arrowpuzzle.game.BuildConfig.DEBUG) {
                    val debugSettings = ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .build()
                    setConsentDebugSettings(debugSettings)
                }
            }
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Log.w(TAG, "Consent form error: ${formError.message}")
                    onComplete()
                    preloadAll(activity)
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.message}")
                onComplete()
            }
        )
    }

    fun preloadAll(context: Context) {
        loadInterstitial(context)
        loadRewardedInterstitial(context)
        loadRewarded(context)
    }

    // ── Interstitial ──────────────────────────────────────────────────────

    private fun loadInterstitial(context: Context) {
        if (interstitialAd != null) return
        InterstitialAd.load(
            context, AdIds.INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    /** Call once per level completion; internally decides whether it's actually time to show one. */
    fun maybeShowInterstitialOnLevelComplete(activity: Activity) {
        levelsSinceLastInterstitial++
        val due = levelsSinceLastInterstitial >= LEVELS_BETWEEN_INTERSTITIALS
        val pastGrace = levelsSinceLastInterstitial >= LEVELS_BEFORE_FIRST_INTERSTITIAL
        if (due && pastGrace) {
            showInterstitial(activity) {}
            levelsSinceLastInterstitial = 0
        }
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            onDismissed()
            loadInterstitial(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial(activity)
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitial(activity)
                onDismissed()
            }
        }
        ad.show(activity)
    }

    // ── Rewarded interstitial (used to grant an extra Hint) ─────────────────

    private fun loadRewardedInterstitial(context: Context) {
        if (rewardedInterstitialAd != null) return
        RewardedInterstitialAd.load(
            context, AdIds.REWARDED_INTERSTITIAL_HINT, AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Rewarded interstitial failed to load: ${error.message}")
                    rewardedInterstitialAd = null
                }
            }
        )
    }

    fun isHintAdReady(): Boolean = rewardedInterstitialAd != null

    fun showRewardedInterstitialForHint(activity: Activity, onEarned: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewardedInterstitialAd
        if (ad == null) {
            onUnavailable()
            loadRewardedInterstitial(activity)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
                loadRewardedInterstitial(activity)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedInterstitialAd = null
                loadRewardedInterstitial(activity)
                onUnavailable()
            }
        }
        ad.show(activity) { earned = true; onEarned() }
        if (!earned) Unit // reward callback fires asynchronously; nothing further needed here
    }

    // ── Rewarded ad (used for "double coins" / "continue" on game over) ────

    private fun loadRewarded(context: Context) {
        if (rewardedAd != null) return
        RewardedAd.load(
            context, AdIds.REWARDED_ARROW, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Rewarded ad failed to load: ${error.message}")
                    rewardedAd = null
                }
            }
        )
    }

    fun isRewardedAdReady(): Boolean = rewardedAd != null

    fun showRewarded(activity: Activity, onEarned: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onUnavailable()
            loadRewarded(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadRewarded(activity)
                onUnavailable()
            }
        }
        ad.show(activity) { onEarned() }
    }
}

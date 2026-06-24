package com.plugin.inappreview

import android.app.Activity
import app.tauri.annotation.Command
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.Plugin
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
//import com.google.android.play.core.review.testing.FakeReviewManager

@TauriPlugin
class InAppReviewPlugin(private val activity: Activity) : Plugin(activity) {
    private val reviewManager: ReviewManager = ReviewManagerFactory.create(activity)
    //private val reviewManager: ReviewManager = FakeReviewManager(activity)
    private var reviewInfo: ReviewInfo? = null

    override fun load(webView: android.webkit.WebView) {
        super.load(webView)
        // android.util.Log.i("InAppReviewPlugin", "Loading plugin...")
        // Pre-fetch review info to improve UX
        prefetchReviewInfo()
    }

    private fun prefetchReviewInfo() {
        // android.util.Log.i("InAppReviewPlugin", "Pre-fetching review info...")
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // android.util.Log.i("InAppReviewPlugin", "Successfully pre-fetched review info")
                reviewInfo = task.result
            // } else {
            //     android.util.Log.e("InAppReviewPlugin", "Failed to pre-fetch review info: ${task.exception?.message}")
            }
        }
    }

    @Command
    fun requestReview(invoke: Invoke) {
        // android.util.Log.i("InAppReviewPlugin", "requestReview logic started")
        // Use pre-fetched reviewInfo if available, otherwise request it
        val currentReviewInfo = reviewInfo

        if (currentReviewInfo != null) {
            // android.util.Log.i("InAppReviewPlugin", "Using pre-fetched review info")
            launchReviewFlow(currentReviewInfo, invoke)
        } else {
            // android.util.Log.i("InAppReviewPlugin", "Pre-fetched review info not available, requesting now...")
            // Fallback: request review info on-demand
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // android.util.Log.i("InAppReviewPlugin", "On-demand request successful")
                    val freshReviewInfo = task.result
                    reviewInfo = freshReviewInfo
                    launchReviewFlow(freshReviewInfo, invoke)
                } else {
                    val error = "Failed to request review: ${task.exception?.message}"
                    // android.util.Log.e("InAppReviewPlugin", error)
                    invoke.reject(error)
                }
            }
        }
    }

    private fun launchReviewFlow(info: ReviewInfo, invoke: Invoke) {
        // android.util.Log.i("InAppReviewPlugin", "Launching review flow...")
        val flow = reviewManager.launchReviewFlow(activity, info)
        flow.addOnCompleteListener { task ->
            // The flow has finished, but we don't know if the user reviewed or not
            // Google Play doesn't provide this information to protect user privacy
            if (task.isSuccessful) {
                // android.util.Log.i("InAppReviewPlugin", "Review flow completed successfully")
                invoke.resolve()
            } else {
                val error = "Failed to launch review flow: ${task.exception?.message}"
                // android.util.Log.e("InAppReviewPlugin", error)
                invoke.reject(error)
            }
        }
    }
}

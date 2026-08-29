package com.gdata.app.domain.model

/**
 * What each mode actually means in G Data.
 * Android does not allow silent traffic rewriting without VPN;
 * these policies drive UI, estimates, recommendations, and system shortcuts.
 */
object ModePolicy {

    fun estimatedSavingsFactor(mode: OptimizationMode, optimizationOn: Boolean): Double {
        if (!optimizationOn) return 0.0
        return when (mode) {
            OptimizationMode.PERFORMANCE -> 0.05
            OptimizationMode.BALANCED -> 0.15
            OptimizationMode.EXTREME -> 0.28
        }
    }

    fun activeSummary(mode: OptimizationMode, optimizationOn: Boolean, gaming: Boolean): String {
        if (!optimizationOn) return "Optimization is paused. Traffic is unrestricted by G Data."
        if (gaming) return "Gaming Mode: prioritizing responsiveness. Background savings relaxed."
        return when (mode) {
            OptimizationMode.PERFORMANCE ->
                "Light monitoring only. Minimal interference. Best for calls, games, and downloads."
            OptimizationMode.BALANCED ->
                "Recommended everyday mode. Tracks usage, suggests limits, and applies moderate savings estimates."
            OptimizationMode.EXTREME ->
                "Strongest savings posture. Prefer system Data Saver and restrict background-heavy apps."
        }
    }

    fun actionHints(mode: OptimizationMode): List<String> {
        return when (mode) {
            OptimizationMode.PERFORMANCE -> listOf(
                "No aggressive background limits",
                "Use when you need full quality video or low lag"
            )
            OptimizationMode.BALANCED -> listOf(
                "Watch high-usage apps in the Apps tab",
                "Keep bundle limits set in Settings"
            )
            OptimizationMode.EXTREME -> listOf(
                "Turn on Android Data Saver",
                "Restrict background data for YouTube, TikTok, etc.",
                "Lower in-app video quality where possible"
            )
        }
    }
}

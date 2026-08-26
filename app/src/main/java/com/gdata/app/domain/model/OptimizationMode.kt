package com.gdata.app.domain.model

enum class OptimizationMode(
    val displayName: String,
    val description: String
) {
    PERFORMANCE(
        displayName = "Performance Mode",
        description = "Maximum performance with minimal optimization."
    ),
    BALANCED(
        displayName = "Balanced Mode",
        description = "Best balance between data savings and performance."
    ),
    EXTREME(
        displayName = "Extreme Data Saver",
        description = "Maximum data savings. Some services may have reduced quality or delayed background activity."
    );

    companion object {
        val DEFAULT = BALANCED
    }
}

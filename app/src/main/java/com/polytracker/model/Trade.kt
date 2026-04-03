package com.polytracker.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Trade(
    val id: String,
    val conditionId: String,
    val marketTitle: String,
    val side: String,       // "BUY" or "SELL"
    val outcome: String,    // "Yes" or "No"
    val shares: Double,
    val price: Double,      // 0.0 – 1.0  (implied probability)
    val timestamp: Long     // unix seconds
) {
    val pricePercent: String get() = "%.1f%%".format(price * 100)
    val totalUsd: String get() = "$%.2f".format(shares * price)
    val sideLabel: String get() = if (side.uppercase() == "BUY") "🟢 BUY" else "🔴 SELL"

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp * 1000))
        }
}

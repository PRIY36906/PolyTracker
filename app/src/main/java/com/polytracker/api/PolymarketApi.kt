package com.polytracker.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.polytracker.model.Trade
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object PolymarketApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // Polymarket CLOB API base URL
    private const val BASE_URL = "https://clob.polymarket.com"
    private const val GAMMA_URL = "https://gamma-api.polymarket.com"

    /**
     * Fetch recent trades for a given wallet address.
     * Uses the Polymarket CLOB trade history endpoint.
     */
    fun fetchTrades(walletAddress: String): List<Trade> {
        return try {
            val url = "$BASE_URL/trades?maker_address=$walletAddress&limit=20"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val result = gson.fromJson(body, TradeResponse::class.java)

            result.data?.map { it.toTrade() } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Fetch market info by condition ID to get a human-readable title.
     */
    fun fetchMarketTitle(conditionId: String): String {
        return try {
            val url = "$GAMMA_URL/markets?condition_id=$conditionId"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return conditionId

            val body = response.body?.string() ?: return conditionId
            val markets = gson.fromJson(body, Array<MarketInfo>::class.java)
            markets.firstOrNull()?.question ?: conditionId
        } catch (e: Exception) {
            conditionId
        }
    }

    // --- Data classes for JSON parsing ---

    data class TradeResponse(
        @SerializedName("data") val data: List<RawTrade>?
    )

    data class RawTrade(
        @SerializedName("id") val id: String,
        @SerializedName("market") val market: String?,          // condition ID
        @SerializedName("asset_id") val assetId: String?,
        @SerializedName("side") val side: String?,              // "BUY" or "SELL"
        @SerializedName("size") val size: String?,              // shares
        @SerializedName("price") val price: String?,            // 0-1 probability
        @SerializedName("outcome") val outcome: String?,        // "Yes" / "No"
        @SerializedName("timestamp") val timestamp: String?,
        @SerializedName("status") val status: String?
    ) {
        fun toTrade(): Trade = Trade(
            id = id,
            conditionId = market ?: "",
            marketTitle = market ?: "Unknown Market",   // enriched later
            side = side ?: "BUY",
            outcome = outcome ?: "Yes",
            shares = size?.toDoubleOrNull() ?: 0.0,
            price = price?.toDoubleOrNull() ?: 0.0,
            timestamp = timestamp?.toLongOrNull() ?: System.currentTimeMillis() / 1000
        )
    }

    data class MarketInfo(
        @SerializedName("question") val question: String?
    )
}

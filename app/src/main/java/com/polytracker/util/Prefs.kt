package com.polytracker.util

import android.content.Context

object Prefs {
    private const val FILE = "polytracker_prefs"
    private const val KEY_WALLET = "wallet_address"
    private const val KEY_LAST_TRADE_ID = "last_trade_id"
    private const val KEY_TRACKING = "is_tracking"

    fun getWallet(ctx: Context): String? =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_WALLET, null)

    fun setWallet(ctx: Context, address: String) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_WALLET, address).apply()

    fun clearWallet(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().remove(KEY_WALLET).apply()

    fun getLastSeenTradeId(ctx: Context): String? =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_LAST_TRADE_ID, null)

    fun setLastSeenTradeId(ctx: Context, id: String) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_TRADE_ID, id).apply()

    fun isTracking(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_TRACKING, false)

    fun setTracking(ctx: Context, value: Boolean) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_TRACKING, value).apply()
}

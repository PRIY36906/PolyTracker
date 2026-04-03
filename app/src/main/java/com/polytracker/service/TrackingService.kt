package com.polytracker.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.polytracker.R
import com.polytracker.api.PolymarketApi
import com.polytracker.model.Trade
import com.polytracker.ui.MainActivity
import com.polytracker.util.Prefs
import kotlinx.coroutines.*

class TrackingService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null

    companion object {
        const val CHANNEL_ID_FG   = "polytracker_fg"
        const val CHANNEL_ID_TRADE = "polytracker_trade"
        const val NOTIF_ID_FG     = 1
        private const val POLL_INTERVAL_MS = 30_000L  // 30 seconds

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID_FG, buildForegroundNotification("Watching trader…"))
        startPolling()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Polling logic
    // -------------------------------------------------------------------------

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                checkForNewTrades()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkForNewTrades() {
        val wallet = Prefs.getWallet(this) ?: return
        val lastSeenId = Prefs.getLastSeenTradeId(this)

        val trades = PolymarketApi.fetchTrades(wallet)
        if (trades.isEmpty()) return

        // Sort newest first (API usually returns newest first, but let's be safe)
        val sorted = trades.sortedByDescending { it.timestamp }
        val newest = sorted.first()

        if (newest.id == lastSeenId) return   // nothing new

        // Enrich with market title
        val enriched = newest.copy(
            marketTitle = PolymarketApi.fetchMarketTitle(newest.conditionId)
        )

        // Persist so we don't re-notify
        Prefs.setLastSeenTradeId(this, newest.id)

        // Broadcast to UI
        val broadcastIntent = Intent("com.polytracker.NEW_TRADE").apply {
            putExtra("trade_id", enriched.id)
            putExtra("market_title", enriched.marketTitle)
            putExtra("side", enriched.side)
            putExtra("outcome", enriched.outcome)
            putExtra("shares", enriched.shares)
            putExtra("price", enriched.price)
            putExtra("timestamp", enriched.timestamp)
        }
        sendBroadcast(broadcastIntent)

        withContext(Dispatchers.Main) {
            sendTradeNotification(enriched)
        }
    }

    // -------------------------------------------------------------------------
    // Notifications
    // -------------------------------------------------------------------------

    private fun buildForegroundNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID_FG)
            .setContentTitle("PolyTracker Active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun sendTradeNotification(trade: Trade) {
        val sideEmoji = if (trade.side.uppercase() == "BUY") "🟢" else "🔴"
        val title = "$sideEmoji Trader just ${trade.side.lowercase()}!"
        val body  = "${trade.marketTitle}\n" +
                    "${trade.outcome} @ ${trade.pricePercent}  •  ${trade.totalUsd}"

        val pi = PendingIntent.getActivity(
            this, trade.id.hashCode(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_TRADE)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setSound(sound)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(System.currentTimeMillis().toInt(), notification)

        // Extra vibration
        vibrate()
    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 300, 150, 300)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(pattern, -1)
        }
    }

    private fun createNotificationChannels() {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Foreground service channel (silent)
        NotificationChannel(CHANNEL_ID_FG, "Tracking Status", NotificationManager.IMPORTANCE_LOW)
            .also { mgr.createNotificationChannel(it) }

        // Trade alert channel (loud)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        NotificationChannel(CHANNEL_ID_TRADE, "Trade Alerts", NotificationManager.IMPORTANCE_HIGH)
            .apply { setSound(sound, attrs); enableVibration(true) }
            .also { mgr.createNotificationChannel(it) }
    }
}

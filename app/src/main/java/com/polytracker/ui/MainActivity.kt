package com.polytracker.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.polytracker.R
import com.polytracker.model.Trade
import com.polytracker.service.TrackingService
import com.polytracker.util.Prefs

class MainActivity : AppCompatActivity() {

    private lateinit var etWallet: EditText
    private lateinit var btnToggle: Button
    private lateinit var tvStatus: TextView
    private lateinit var recyclerView: RecyclerView
    private val adapter = TradeAdapter()

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled silently */ }

    private val tradeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val trade = Trade(
                id           = intent.getStringExtra("trade_id") ?: return,
                conditionId  = "",
                marketTitle  = intent.getStringExtra("market_title") ?: "Unknown",
                side         = intent.getStringExtra("side") ?: "BUY",
                outcome      = intent.getStringExtra("outcome") ?: "Yes",
                shares       = intent.getDoubleExtra("shares", 0.0),
                price        = intent.getDoubleExtra("price", 0.0),
                timestamp    = intent.getLongExtra("timestamp", 0L)
            )
            adapter.addTrade(trade)
            recyclerView.scrollToPosition(0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etWallet     = findViewById(R.id.etWallet)
        btnToggle    = findViewById(R.id.btnToggle)
        tvStatus     = findViewById(R.id.tvStatus)
        recyclerView = findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Restore saved wallet
        Prefs.getWallet(this)?.let { etWallet.setText(it) }

        // Restore UI state
        updateUI(Prefs.isTracking(this))

        btnToggle.setOnClickListener {
            if (Prefs.isTracking(this)) stopTracking()
            else startTracking()
        }

        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.polytracker.NEW_TRADE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(tradeReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(tradeReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(tradeReceiver)
    }

    private fun startTracking() {
        val wallet = etWallet.text.toString().trim()
        if (wallet.length < 10) {
            Toast.makeText(this, "Enter a valid wallet address", Toast.LENGTH_SHORT).show()
            return
        }
        Prefs.setWallet(this, wallet)
        Prefs.setTracking(this, true)
        TrackingService.start(this)
        updateUI(true)
    }

    private fun stopTracking() {
        Prefs.setTracking(this, false)
        TrackingService.stop(this)
        updateUI(false)
    }

    private fun updateUI(tracking: Boolean) {
        if (tracking) {
            btnToggle.text = "Stop Tracking"
            btnToggle.setBackgroundColor(getColor(android.R.color.holo_red_light))
            tvStatus.text = "🟢 Tracking active — checking every 30s"
        } else {
            btnToggle.text = "Start Tracking"
            btnToggle.setBackgroundColor(getColor(android.R.color.holo_green_light))
            tvStatus.text = "⚪ Not tracking"
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

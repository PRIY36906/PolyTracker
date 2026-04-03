package com.polytracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.polytracker.R
import com.polytracker.model.Trade

class TradeAdapter(private val trades: MutableList<Trade> = mutableListOf()) :
    RecyclerView.Adapter<TradeAdapter.TradeViewHolder>() {

    class TradeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSide: TextView      = view.findViewById(R.id.tvSide)
        val tvMarket: TextView    = view.findViewById(R.id.tvMarket)
        val tvDetails: TextView   = view.findViewById(R.id.tvDetails)
        val tvTime: TextView      = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TradeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trade, parent, false)
        return TradeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TradeViewHolder, position: Int) {
        val trade = trades[position]
        holder.tvSide.text    = trade.sideLabel
        holder.tvMarket.text  = trade.marketTitle
        holder.tvDetails.text = "${trade.outcome}  •  ${trade.pricePercent}  •  ${trade.totalUsd}"
        holder.tvTime.text    = trade.formattedTime
    }

    override fun getItemCount(): Int = trades.size

    fun addTrade(trade: Trade) {
        trades.add(0, trade)
        notifyItemInserted(0)
    }

    fun setTrades(newTrades: List<Trade>) {
        trades.clear()
        trades.addAll(newTrades)
        notifyDataSetChanged()
    }
}

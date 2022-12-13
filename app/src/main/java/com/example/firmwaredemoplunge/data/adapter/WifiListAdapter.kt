package com.example.firmwaredemoplunge.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.firmwaredemoplunge.R
import com.example.firmwaredemoplunge.data.model.WfiNameList

class WifiListAdapter(
    val list: ArrayList<WfiNameList.WfiNameListItem>,
    val listener: IwifiConnect,
) :
    RecyclerView.Adapter<WifiListAdapter.WifiItem>() {


    inner class WifiItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById<TextView>(R.id.tvName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiItem {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.wifi_item, parent, false)
        return WifiItem(view)
    }

    override fun onBindViewHolder(holder: WifiItem, position: Int) {

        list.sortBy { it.wifi_rssi }

        holder.name.text = list[position].wifi_name
        holder

        holder.itemView.setOnClickListener {
            listener.wifiItemClick(list[position].wifi_name)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface IwifiConnect {
        fun wifiItemClick(name: String)
    }
}



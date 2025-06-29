package com.nexoplus.tagonboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nexoplus.tagonboarding.databinding.ItemAccessLogBinding

class AccessLogsAdapter(private val logs: List<AccessLog>) : RecyclerView.Adapter<AccessLogsAdapter.AccessLogViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccessLogViewHolder {
        val binding = ItemAccessLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccessLogViewHolder(binding)
    }
    override fun onBindViewHolder(holder: AccessLogViewHolder, position: Int) {
        holder.bind(logs[position])
    }
    override fun getItemCount() = logs.size
    class AccessLogViewHolder(val binding: ItemAccessLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(log: AccessLog) {
            binding.accessTimeText.text = log.accessTime
            binding.userNameText.text = log.userName
            binding.vehicleNumberText.text = log.vehicleNumber
            binding.tagIdText.text = log.tagId
            binding.laneText.text = log.lane
            binding.deviceText.text = log.device
            binding.statusText.text = log.status
        }
    }
} 
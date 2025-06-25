package com.nexoplus.tagonboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.loginapp.databinding.ItemAnprUserBinding

class AnprUsersAdapter(private val users: List<AnprUser>) : RecyclerView.Adapter<AnprUsersAdapter.AnprUserViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnprUserViewHolder {
        val binding = ItemAnprUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnprUserViewHolder(binding)
    }
    override fun onBindViewHolder(holder: AnprUserViewHolder, position: Int) {
        holder.bind(users[position])
    }
    override fun getItemCount() = users.size
    class AnprUserViewHolder(private val binding: ItemAnprUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: AnprUser) {
            binding.nameText.text = user.name
            binding.vehicleNumberText.text = user.vehicleNumber
            binding.createdAtText.text = user.createdAt
            binding.activeStatusText.text = if (user.isActive) "Active" else "Inactive"
        }
    }
} 
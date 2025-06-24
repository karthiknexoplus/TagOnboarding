package com.example.loginapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.loginapp.databinding.ActivityMenuBinding
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.loginapp.databinding.ItemMenuBinding

class MenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val menuItems = listOf(
            MenuItem(R.drawable.ic_tag, "Tag Onboarding", HomeActivity::class.java),
            MenuItem(R.drawable.ic_users, "Show Users", UsersActivity::class.java),
            MenuItem(R.drawable.ic_barrier, "Open Barrier", BarrierControlActivity::class.java),
            MenuItem(R.drawable.ic_lane, "Lane Info", LaneInfoActivity::class.java),
            MenuItem(R.drawable.ic_anpr_data, "ANPR Data", ANPRDataActivity::class.java),
            MenuItem(R.drawable.ic_anpr_users, "ANPR Users", AnprUsersActivity::class.java),
            MenuItem(R.drawable.ic_logs, "Access Logs", AccessLogsActivity::class.java),
            MenuItem(R.drawable.ic_reader, "Reader Settings", ReaderSettingsActivity::class.java),
            MenuItem(R.drawable.ic_reader, "SSH Terminal", SshTerminalActivity::class.java)
        )
        val adapter = MenuAdapter(menuItems) { item ->
            startActivity(Intent(this, item.targetActivity))
        }
        binding.menuRecyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.menuRecyclerView.adapter = adapter

        binding.logoutButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}

data class MenuItem(val iconRes: Int, val label: String, val targetActivity: Class<*>)

class MenuAdapter(private val items: List<MenuItem>, private val onClick: (MenuItem) -> Unit) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }
    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }
    override fun getItemCount() = items.size
    class MenuViewHolder(private val binding: ItemMenuBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MenuItem, onClick: (MenuItem) -> Unit) {
            binding.menuIcon.setImageResource(item.iconRes)
            binding.menuLabel.text = item.label
            binding.root.setOnClickListener { onClick(item) }
        }
    }
} 
package com.nexoplus.tagonboarding

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexoplus.tagonboarding.databinding.ActivityAnprUsersBinding
import okhttp3.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException

class AnprUsersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnprUsersBinding
    private lateinit var adapter: AnprUsersAdapter
    private val anprUsers = mutableListOf<AnprUser>()
    private val logTag = "NexoplusAnprUsers"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnprUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AnprUsersAdapter(anprUsers)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadAnprUsers()

        binding.filterEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAnprUsers(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadAnprUsers() {
        val client = OkHttpClient()
        val url = "http://136.232.224.78:5000/api/get_anpr_users"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "curl/7.64.1")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(logTag, "Failed to load ANPR users: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@AnprUsersActivity, "Failed to load ANPR users: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d(logTag, "Response code: ${response.code}, body: $body")
                runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        try {
                            val root = JSONObject(body)
                            val usersArray = root.getJSONArray("users")
                            anprUsers.clear()
                            for (i in 0 until usersArray.length()) {
                                val obj = usersArray.getJSONObject(i)
                                anprUsers.add(
                                    AnprUser(
                                        obj.optString("name"),
                                        obj.optString("vehicle_number"),
                                        obj.optString("created_at"),
                                        obj.optBoolean("is_active")
                                    )
                                )
                            }
                            adapter.notifyDataSetChanged()
                            binding.totalUsersText.text = "Total ANPR users: ${anprUsers.size}"
                        } catch (e: Exception) {
                            Toast.makeText(this@AnprUsersActivity, "Parse error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@AnprUsersActivity, "Failed to load ANPR users: Invalid response", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun filterAnprUsers(query: String) {
        val filtered = if (query.isBlank()) anprUsers else anprUsers.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.vehicleNumber.contains(query, ignoreCase = true)
        }
        adapter = AnprUsersAdapter(filtered)
        binding.recyclerView.adapter = adapter
        binding.totalUsersText.text = "Total ANPR users: ${filtered.size}"
    }
}

data class AnprUser(val name: String, val vehicleNumber: String, val createdAt: String, val isActive: Boolean) 
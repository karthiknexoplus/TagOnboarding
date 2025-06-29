package com.nexoplus.tagonboarding

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nexoplus.tagonboarding.databinding.ActivityBarrierControlBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class BarrierControlActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBarrierControlBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarrierControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.openBarrierButton.setOnClickListener {
            openBarrier()
        }
    }

    private fun openBarrier() {
        val client = OkHttpClient()
        val url = "http://136.232.224.78:5000/api/barrier-control"
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        val json = JSONObject().apply {
            put("lane_id", 1)
            put("device_id", 1)
            put("action", "open")
            put("timestamp", timestamp)
        }
        val requestBody = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()
        binding.responseText.text = "Sending request..."
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.responseText.text = "Failed: ${e.message}"
                    Toast.makeText(this@BarrierControlActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "No response body"
                runOnUiThread {
                    if (response.isSuccessful) {
                        binding.responseText.text = "Barrier opened successfully!\nResponse: $body"
                    } else {
                        binding.responseText.text = "Failed to open barrier.\nResponse: $body"
                    }
                }
            }
        })
    }
} 
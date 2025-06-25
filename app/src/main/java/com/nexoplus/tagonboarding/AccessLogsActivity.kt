package com.nexoplus.tagonboarding

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.loginapp.databinding.ActivityAccessLogsBinding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AccessLogsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccessLogsBinding
    private lateinit var adapter: AccessLogsAdapter
    private val logs = mutableListOf<AccessLog>()
    private val filteredLogs = mutableListOf<AccessLog>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccessLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AccessLogsAdapter(filteredLogs)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        val today = Calendar.getInstance()
        val start = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        binding.tvFromDate.text = dateFormat.format(start.time)
        binding.tvToDate.text = dateFormat.format(today.time)

        val statusAdapter = ArrayAdapter.createFromResource(this, R.array.access_log_status, android.R.layout.simple_spinner_item)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = statusAdapter

        binding.btnFromDate.setOnClickListener { pickDate(binding.tvFromDate) }
        binding.btnToDate.setOnClickListener { pickDate(binding.tvToDate) }
        binding.btnSearch.setOnClickListener { fetchLogs() }

        binding.filterEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLogs(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun pickDate(target: View) {
        val calendar = Calendar.getInstance()
        val listener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            (target as? android.widget.TextView)?.text = dateFormat.format(calendar.time)
        }
        DatePickerDialog(this, listener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun fetchLogs() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE
        logs.clear()
        filteredLogs.clear()
        adapter.notifyDataSetChanged()

        val from = binding.tvFromDate.text.toString()
        val to = binding.tvToDate.text.toString()
        val status = binding.spinnerStatus.selectedItem.toString().lowercase(Locale.US)
        val perPage = 100 // or any reasonable number
        var totalCount = 0

        fun fetchPage(page: Int) {
            val url = "http://136.232.224.78:5000/viewonmobile_access_logs?start_date=$from&end_date=$to&status=$status&page=$page&per_page=$perPage"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.errorText.text = "Failed: ${e.message}"
                    binding.errorText.visibility = View.VISIBLE
                        binding.totalLogsText.text = "Total logs: 0"
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        val json = JSONObject(body)
                        if (json.optString("status") == "success") {
                            val arr = json.optJSONArray("logs")
                                totalCount = json.optInt("total_count", 0)
                            if (arr != null) {
                                for (i in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(i)
                                    val user = obj.getJSONObject("user")
                                    logs.add(AccessLog(
                                        obj.optString("access_time"),
                                        user.optString("name"),
                                        user.optString("vehicle_number"),
                                        obj.optString("tag_id"),
                                        obj.optString("lane"),
                                        obj.optString("device"),
                                        obj.optString("status")
                                    ))
                                }
                                    // If we haven't fetched all logs, fetch next page
                                    if (logs.size < totalCount && arr.length() > 0) {
                                        fetchPage(page + 1)
                                    } else {
                                        binding.progressBar.visibility = View.GONE
                                filterLogs(binding.filterEditText.text.toString())
                                binding.errorText.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
                                binding.errorText.text = if (logs.isEmpty()) "No logs found" else ""
                                        adapter.notifyDataSetChanged()
                                        binding.totalLogsText.text = "Total logs: $totalCount"
                                    }
                                } else {
                                    binding.progressBar.visibility = View.GONE
                                    binding.errorText.text = "No logs found"
                                    binding.errorText.visibility = View.VISIBLE
                                    binding.totalLogsText.text = "Total logs: 0"
                                }
                            } else {
                                binding.progressBar.visibility = View.GONE
                                binding.errorText.text = "No logs found"
                                binding.errorText.visibility = View.VISIBLE
                                binding.totalLogsText.text = "Total logs: 0"
                            }
                        } else {
                            binding.progressBar.visibility = View.GONE
                            binding.errorText.text = "No logs found"
                            binding.errorText.visibility = View.VISIBLE
                            binding.totalLogsText.text = "Total logs: 0"
                    }
                }
            }
        })
        }

        fetchPage(1)
    }

    private fun filterLogs(query: String) {
        filteredLogs.clear()
        if (query.isBlank()) {
            filteredLogs.addAll(logs)
        } else {
            val lower = query.lowercase(Locale.US)
            filteredLogs.addAll(logs.filter {
                it.userName.lowercase(Locale.US).contains(lower) ||
                it.vehicleNumber.lowercase(Locale.US).contains(lower) ||
                it.tagId.lowercase(Locale.US).contains(lower)
            })
        }
        adapter.notifyDataSetChanged()
        binding.totalLogsText.text = "Total logs: ${filteredLogs.size}"
    }
}

data class AccessLog(
    val accessTime: String,
    val userName: String,
    val vehicleNumber: String,
    val tagId: String,
    val lane: String,
    val device: String,
    val status: String
) 
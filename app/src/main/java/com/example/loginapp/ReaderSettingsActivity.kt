package com.example.loginapp

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.loginapp.databinding.ActivityReaderSettingsBinding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType

class ReaderSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReaderSettingsBinding
    private val readers = listOf(
        Reader("Reader 1", "192.168.60.250", 1),
        Reader("Reader 2", "192.168.60.251", 2)
    )
    private var selectedReader: Reader = readers[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = object : ArrayAdapter<Reader>(this, android.R.layout.simple_spinner_item, readers) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as android.widget.TextView
                val reader = getItem(position)
                view.text = "${reader?.name} (${reader?.ip})"
                return view
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as android.widget.TextView
                val reader = getItem(position)
                view.text = "${reader?.name} (${reader?.ip})"
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.readerDropdown.adapter = adapter
        binding.readerDropdown.setSelection(0)
        binding.readerDropdown.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedReader = readers[position]
                binding.readerIpText.text = selectedReader.ip
                binding.rfPowerSpinner.setSelection(0)
                binding.statusText.text = ""
                binding.currentRfPowerText.text = "--"
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })

        // Set up RF Power Spinner (1-30)
        val rfPowerValues = (1..30).map { it.toString() }
        val rfPowerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rfPowerValues)
        rfPowerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.rfPowerSpinner.adapter = rfPowerAdapter
        binding.rfPowerSpinner.setSelection(0)

        binding.getButton.setOnClickListener { getRfPower() }
        binding.setButton.setOnClickListener { setRfPower() }
    }

    private fun getRfPower() {
        binding.progressBar.visibility = View.VISIBLE
        val url = "http://136.232.224.78:5000/api/rfid/rfpower?reader=${selectedReader.id}"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.statusText.text = "Failed: ${e.message}"
                    Toast.makeText(this@ReaderSettingsActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && body != null) {
                        try {
                            val json = JSONObject(body)
                            val rfPower = json.optInt("rf_power", -1)
                            binding.currentRfPowerText.text = rfPower.toString()
                            binding.statusText.text = "Success"
                        } catch (e: Exception) {
                            binding.statusText.text = "Parse error: ${e.message}"
                        }
                    } else {
                        binding.statusText.text = "Failed: ${response.message}"
                    }
                }
            }
        })
    }

    private fun setRfPower() {
        val rfPower = binding.rfPowerSpinner.selectedItem.toString().toIntOrNull()
        if (rfPower == null || rfPower < 1 || rfPower > 30) {
            binding.statusText.text = "Select RF Power (1-30)"
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        val url = "http://136.232.224.78:5000/api/rfid/rfpower"
        val client = OkHttpClient()
        val json = JSONObject().apply {
            put("reader", selectedReader.id)
            put("rf_power", rfPower)
        }
        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder().url(url).post(body).addHeader("Content-Type", "application/json").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.statusText.text = "Failed: ${e.message}"
                    Toast.makeText(this@ReaderSettingsActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && body != null) {
                        try {
                            val json = JSONObject(body)
                            val status = json.optString("status", "")
                            val rfPower = json.optInt("rf_power", -1)
                            if (status == "success") {
                                binding.currentRfPowerText.text = rfPower.toString()
                                binding.statusText.text = "Set successfully"
                            } else {
                                binding.statusText.text = "Failed: $status"
                            }
                        } catch (e: Exception) {
                            binding.statusText.text = "Parse error: ${e.message}"
                        }
                    } else {
                        binding.statusText.text = "Failed: ${response.message}"
                    }
                }
            }
        })
    }
}

data class Reader(val name: String, val ip: String, val id: Int) 
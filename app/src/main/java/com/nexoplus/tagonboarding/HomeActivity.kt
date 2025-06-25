package com.nexoplus.tagonboarding

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.loginapp.databinding.ActivityHomeBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var torchOn = false

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val qrContent = result.contents
            val fastagId =
                if (qrContent.contains("pn=")) {
                    val start = qrContent.indexOf("pn=") + 3
                    val end = qrContent.indexOf('&', start).let { if (it == -1) qrContent.length else it }
                    qrContent.substring(start, end)
                } else qrContent
            binding.fastagIdInput.setText(fastagId)
            binding.qrResultText.text = "Fastag ID: $fastagId"
        } else {
            binding.qrResultText.text = "No QR code scanned."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.welcomeText.text = "Welcome, admin!"

        binding.scanQrButton.setOnClickListener {
            val options = ScanOptions()
            options.setPrompt("Scan a QR or Bar code")
            options.setBeepEnabled(true)
            options.setOrientationLocked(true)
            options.setCaptureActivity(PortraitCaptureActivity::class.java)
            barcodeLauncher.launch(options)
        }

        binding.torchToggleButton.setOnClickListener {
            torchOn = !torchOn
            val options = ScanOptions()
            options.setPrompt("Scan a QR or Bar code")
            options.setBeepEnabled(true)
            options.setOrientationLocked(true)
            options.setCaptureActivity(PortraitCaptureActivity::class.java)
            options.setTorchEnabled(torchOn)
            barcodeLauncher.launch(options)
        }

        binding.submitButton.setOnClickListener {
            val name = binding.nameInput.text.toString()
            val designation = binding.designationInput.text.toString()
            val vehicleNumber = binding.vehicleNumberInput.text.toString()
            val fastagId = binding.fastagIdInput.text.toString()

            if (fastagId.isEmpty() || name.isEmpty() || designation.isEmpty() || vehicleNumber.isEmpty()) {
                Toast.makeText(this, "Please fill all fields and scan QR", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val json = JSONObject().apply {
                put("name", name)
                put("designation", designation)
                put("vehicle_number", vehicleNumber)
                put("fastag_id", fastagId)
                put("location_id", 1)
                put("kyc_document_type", "Aadhaar")
                put("kyc_document_number", "123")
                put("valid_from", "2020-01-01")
                put("valid_to", "2080-01-01")
                put("is_active", true)
                put("access_permissions", JSONArray().apply {
                    put(JSONObject().apply {
                        put("lane_id", 1)
                        put("start_time", "00:00")
                        put("end_time", "23:59")
                        put("days_of_week", "Mon,Tue,Wed,Thu,Fri")
                    })
                })
            }

            postUserData(json)
        }
    }

    private fun postUserData(json: JSONObject) {
        val client = OkHttpClient()
        val requestBody = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("http://136.232.224.78:5000/api/users")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Failed to submit: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: "No response body"
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@HomeActivity, "Submitted successfully! Response: $responseBody", Toast.LENGTH_LONG).show()
                        // Clear all fields
                        binding.nameInput.text?.clear()
                        binding.designationInput.text?.clear()
                        binding.vehicleNumberInput.text?.clear()
                        binding.fastagIdInput.text?.clear()
                        binding.qrResultText.text = "QR Result will appear here"
                    } else {
                        Toast.makeText(this@HomeActivity, "Submission failed: ${response.message}. Response: $responseBody", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
} 
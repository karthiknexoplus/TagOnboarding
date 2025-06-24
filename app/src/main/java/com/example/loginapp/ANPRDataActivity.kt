package com.example.loginapp

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loginapp.databinding.ActivityAnprDataBinding
import com.example.loginapp.databinding.ItemAnprResultBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import android.util.Log
import android.widget.Spinner
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher

class ANPRDataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnprDataBinding
    private lateinit var adapter: ANPRAdapter
    private val anprList = mutableListOf<ANPRResult>()
    private val cameraList = listOf(
        Camera("Camera1", "192.168.60.253"),
        Camera("Camera2", "192.168.60.254")
    )
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val logTag = "ANPRDataActivity"
    private val cacheExpiryMs = 5 * 60 * 1000L // 5 minutes
    private val cacheMap = mutableMapOf<String, Long>() // cacheKey -> timestamp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnprDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Camera dropdown (Spinner) with name and IP
        val cameraAdapter = object : ArrayAdapter<Camera>(this, android.R.layout.simple_spinner_item, cameraList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val camera = getItem(position)
                view.text = "${camera?.name} (${camera?.ip})"
                return view
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                val camera = getItem(position)
                view.text = "${camera?.name} (${camera?.ip})"
                return view
            }
        }
        cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.cameraDropdown.adapter = cameraAdapter
        binding.cameraDropdown.setSelection(0)

        // Date pickers
        val calendar = Calendar.getInstance()
        binding.fromDateInput.setOnClickListener {
            showDatePicker(binding.fromDateInput, calendar)
        }
        binding.toDateInput.setOnClickListener {
            showDatePicker(binding.toDateInput, calendar)
        }
        binding.fromDateInput.setText(dateFormat.format(calendar.time))
        binding.toDateInput.setText(dateFormat.format(calendar.time))

        // RecyclerView
        adapter = ANPRAdapter(anprList, this)
        binding.anprRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.anprRecyclerView.adapter = adapter

        binding.searchButton.setOnClickListener {
            val cameraObj = binding.cameraDropdown.selectedItem as? Camera
            val camera = cameraObj?.name ?: ""
            val fromDate = binding.fromDateInput.text.toString()
            val toDate = binding.toDateInput.text.toString()
            Log.d(logTag, "Search clicked: camera=$camera, fromDate=$fromDate, toDate=$toDate")
            if (camera.isBlank() || fromDate.isBlank() || toDate.isBlank()) {
                Toast.makeText(this, "Please select camera and dates", Toast.LENGTH_SHORT).show()
                Log.e(logTag, "Missing camera or dates")
                return@setOnClickListener
            }
            binding.progressBar.visibility = View.VISIBLE
            binding.searchButton.isEnabled = false
            anprList.clear()
            adapter.notifyDataSetChanged()
            searchANPR(camera, fromDate, toDate, useCache = true)
        }
        // Add a refresh button to bypass cache
        binding.refreshButton?.setOnClickListener {
            val cameraObj = binding.cameraDropdown.selectedItem as? Camera
            val camera = cameraObj?.name ?: ""
            val fromDate = binding.fromDateInput.text.toString()
            val toDate = binding.toDateInput.text.toString()
            Log.d(logTag, "Refresh clicked: camera=$camera, fromDate=$fromDate, toDate=$toDate")
            if (camera.isBlank() || fromDate.isBlank() || toDate.isBlank()) {
                Toast.makeText(this, "Please select camera and dates", Toast.LENGTH_SHORT).show()
                Log.e(logTag, "Missing camera or dates")
                return@setOnClickListener
            }
            binding.progressBar.visibility = View.VISIBLE
            binding.searchButton.isEnabled = false
            anprList.clear()
            adapter.notifyDataSetChanged()
            searchANPR(camera, fromDate, toDate, useCache = false)
        }

        binding.filterVehicleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAnprResults(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun showDatePicker(target: View, calendar: Calendar) {
        val editText = target as android.widget.EditText
        val listener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            editText.setText(dateFormat.format(calendar.time))
        }
        val now = Calendar.getInstance()
        DatePickerDialog(this, listener, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun searchANPR(camera: String, fromDate: String, toDate: String, useCache: Boolean = true) {
        val cacheKey = "${camera}_${fromDate}_${toDate}".replace("/", "-")
        val cacheDir = File(filesDir, "anpr_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val cacheFile = File(cacheDir, "$cacheKey.json")
        val now = System.currentTimeMillis()
        val cacheValid = cacheFile.exists() && cacheMap[cacheKey]?.let { now - it < cacheExpiryMs } == true
        if (useCache && cacheValid) {
            Log.d(logTag, "Cache hit for $cacheKey, loading from cache")
            val json = cacheFile.readText()
            Log.d(logTag, "Full cached JSON: $json")
            runOnUiThread {
                updateResultsFromJson(json, cacheDir)
                binding.progressBar.visibility = View.GONE
                binding.searchButton.isEnabled = true
            }
            return
        }
        val url = "http://136.232.224.78:5000/api/archive-search?camera=$camera&start_date=$fromDate&end_date=$toDate"
        Log.d(logTag, "Cache miss or refresh, calling API: $url")
        thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                Log.d(logTag, "Full API response: $body") // Log full JSON
                if (response.isSuccessful && body != null) {
                    // Only cache if results are non-empty
                    val root = org.json.JSONObject(body)
                    val arr = root.optJSONArray("results") ?: root.optJSONArray("data") ?: JSONArray()
                    if (arr.length() > 0) {
                        cacheFile.writeText(body)
                        cacheMap[cacheKey] = now
                        Log.d(logTag, "Cached response for $cacheKey at $now")
                    } else {
                        Log.d(logTag, "Not caching empty results for $cacheKey")
                    }
                    runOnUiThread {
                        updateResultsFromJson(body, cacheDir)
                        binding.progressBar.visibility = View.GONE
                        binding.searchButton.isEnabled = true
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "No data found", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                        binding.searchButton.isEnabled = true
                    }
                    Log.e(logTag, "API call failed: code=${response.code}, body=${body}")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    binding.searchButton.isEnabled = true
                }
                Log.e(logTag, "Exception during API call: ${e.message}", e)
            }
        }
    }

    private fun updateResultsFromJson(json: String, cacheDir: File) {
        anprList.clear()
        try {
            val root = org.json.JSONObject(json)
            val arr = root.optJSONArray("results") ?: root.optJSONArray("data") ?: JSONArray()
            Log.d(logTag, "Parsing ${arr.length()} ANPR results from JSON")
            // Update total records text
            binding.totalRecordsText.text = "Total records: ${arr.length()}"
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val imageUrl = obj.optString("image_url")
                val imageFilename = obj.optString("image_filename")
                // Add result with no image first
                val result = ANPRResult(
                    obj.optString("vehiclePlate"),
                    obj.optString("time"),
                    null,
                    obj.optString("lane"),
                    obj.optString("camera"),
                    obj.optString("ip")
                )
                anprList.add(result)
                adapter.notifyItemInserted(anprList.size - 1)
                // Download image in background
                if (imageUrl.isNotBlank() && imageFilename.isNotBlank()) {
                    thread {
                        val localImage = downloadAndCacheImage(imageUrl, imageFilename, cacheDir)
                        if (localImage != null) {
                            runOnUiThread {
                                result.imagePath = localImage
                                adapter.notifyItemChanged(anprList.size - 1)
                            }
                        }
                    }
                }
                Log.d(logTag, "Result $i: plate=${obj.optString("vehiclePlate")}, time=${obj.optString("time")}, imageUrl=$imageUrl, localImage=null (downloading async)")
            }
            adapter.notifyDataSetChanged()
            Log.d(logTag, "UI updated with ${anprList.size} results")
        } catch (e: Exception) {
            Toast.makeText(this, "Parse error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(logTag, "Parse error: ${e.message}", e)
            // Show 0 if error
            binding.totalRecordsText.text = "Total records: 0"
        }
    }

    private fun downloadAndCacheImage(imageUrl: String, imageFilename: String, cacheDir: File): String? {
        val localFile = File(cacheDir, imageFilename)
        if (localFile.exists()) {
            Log.d(logTag, "Image cache hit: $imageFilename")
            return localFile.absolutePath
        }
        if (imageUrl.isBlank()) {
            Log.e(logTag, "Image URL is blank for $imageFilename")
            return null
        }
        val fullUrl = "http://136.232.224.78:5000$imageUrl"
        return try {
            val client = OkHttpClient()
            val request = Request.Builder().url(fullUrl).build()
            val response = client.newCall(request).execute()
            val bytes = response.body?.bytes()
            if (response.isSuccessful && bytes != null) {
                val fos = FileOutputStream(localFile)
                fos.write(bytes)
                fos.close()
                Log.d(logTag, "Downloaded and cached image: $imageFilename")
                localFile.absolutePath
            } else {
                Log.e(logTag, "Failed to download image: $fullUrl, code=${response.code}")
                null
            }
        } catch (e: IOException) {
            Log.e(logTag, "Exception downloading image: $fullUrl, ${e.message}", e)
            null
        }
    }

    private fun filterAnprResults(query: String) {
        val filtered = if (query.isBlank()) anprList else anprList.filter {
            it.plate.contains(query, ignoreCase = true)
        }
        adapter = ANPRAdapter(filtered.toMutableList(), this)
        binding.anprRecyclerView.adapter = adapter
        binding.totalRecordsText.text = "Total records: ${filtered.size}"
    }
}

data class ANPRResult(val plate: String, val time: String, var imagePath: String?, val lane: String?, val camera: String?, val ip: String?)

data class Camera(val name: String, val ip: String)

class ANPRAdapter(private val items: List<ANPRResult>, private val context: Context) : RecyclerView.Adapter<ANPRAdapter.ANPRViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ANPRViewHolder {
        val binding = ItemAnprResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ANPRViewHolder(binding)
    }
    override fun onBindViewHolder(holder: ANPRViewHolder, position: Int) {
        holder.bind(items[position], context)
        holder.itemView.setOnClickListener {
            // Collect all image paths, plates, and times
            val imagePaths = items.map { it.imagePath ?: "" }
            val plates = items.map { it.plate ?: "" }
            val times = items.map { it.time ?: "" }
            val intent = Intent(context, ImagePreviewActivity::class.java)
            intent.putStringArrayListExtra("imagePaths", ArrayList(imagePaths))
            intent.putStringArrayListExtra("plates", ArrayList(plates))
            intent.putStringArrayListExtra("times", ArrayList(times))
            intent.putExtra("currentIndex", position)
            context.startActivity(intent)
        }
    }
    override fun getItemCount() = items.size
    class ANPRViewHolder(private val binding: ItemAnprResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ANPRResult, context: Context) {
            binding.plateText.text = item.plate
            binding.timeText.text = item.time
            if (item.imagePath != null) {
                val bmp = BitmapFactory.decodeFile(item.imagePath)
                binding.anprImage.setImageBitmap(bmp)
            } else {
                binding.anprImage.setImageResource(android.R.drawable.ic_menu_report_image)
            }

            // Share button logic
            binding.shareButton.setOnClickListener {
                if (item.imagePath != null) {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "image/*"
                    val file = File(item.imagePath)
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        context.applicationContext.packageName + ".provider",
                        file
                    )
                    val details = "Vehicle: ${item.plate}\nTime: ${item.time}\nLane: ${item.lane ?: "-"}\nCamera: ${item.camera ?: "-"}"
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    shareIntent.putExtra(Intent.EXTRA_TEXT, details)
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(shareIntent, "Share ANPR Image"))
                } else {
                    Toast.makeText(context, "Image not available", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 
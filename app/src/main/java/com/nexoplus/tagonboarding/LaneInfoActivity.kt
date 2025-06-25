package com.nexoplus.tagonboarding

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.loginapp.databinding.ActivityLaneInfoBinding
import com.example.loginapp.databinding.ItemLaneBinding
import com.example.loginapp.databinding.ItemDeviceBinding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class LaneInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLaneInfoBinding
    private lateinit var adapter: LaneAdapter
    private val lanes = mutableListOf<Lane>()
    private val logTag = "NexoplusLaneInfo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaneInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = LaneAdapter(lanes)
        binding.laneRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.laneRecyclerView.adapter = adapter

        fetchLaneInfo()
    }

    private fun fetchLaneInfo() {
        val client = OkHttpClient()
        val url = "http://136.232.224.78:5000/api/lane-device-counts"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(logTag, "Failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@LaneInfoActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d(logTag, "Response: $body")
                runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        try {
                            val root = JSONObject(body)
                            val data = root.getJSONObject("data")
                            val lanesArray = data.getJSONArray("lanes")
                            lanes.clear()
                            for (i in 0 until lanesArray.length()) {
                                val laneObj = lanesArray.getJSONObject(i)
                                val devicesArray = laneObj.getJSONArray("devices")
                                val devices = mutableListOf<Device>()
                                for (j in 0 until devicesArray.length()) {
                                    val dev = devicesArray.getJSONObject(j)
                                    devices.add(Device(
                                        dev.optInt("device_id"),
                                        dev.optString("device_name"),
                                        dev.optString("device_type"),
                                        dev.optString("status"),
                                        dev.optString("ip_address"),
                                        dev.optInt("port")
                                    ))
                                }
                                lanes.add(Lane(
                                    laneObj.optInt("lane_id"),
                                    laneObj.optString("lane_name"),
                                    laneObj.optInt("total_devices"),
                                    devices
                                ))
                            }
                            adapter.notifyDataSetChanged()
                        } catch (e: Exception) {
                            Toast.makeText(this@LaneInfoActivity, "Parse error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@LaneInfoActivity, "Failed to load lane info", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}

data class Lane(val laneId: Int, val laneName: String, val totalDevices: Int, val devices: List<Device>)
data class Device(val deviceId: Int, val deviceName: String, val deviceType: String, val status: String, val ipAddress: String, val port: Int)

class LaneAdapter(private val lanes: List<Lane>) : RecyclerView.Adapter<LaneAdapter.LaneViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LaneViewHolder {
        val binding = ItemLaneBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LaneViewHolder(binding)
    }
    override fun onBindViewHolder(holder: LaneViewHolder, position: Int) {
        holder.bind(lanes[position])
    }
    override fun getItemCount() = lanes.size
    class LaneViewHolder(private val binding: ItemLaneBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lane: Lane) {
            binding.laneNameText.text = "${lane.laneName} (ID: ${lane.laneId})"
            binding.totalDevicesText.text = "Total devices: ${lane.totalDevices}"
            val deviceAdapter = DeviceAdapter(lane.devices)
            binding.deviceRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)
            binding.deviceRecyclerView.adapter = deviceAdapter
        }
    }
}

class DeviceAdapter(private val devices: List<Device>) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }
    override fun getItemCount() = devices.size
    class DeviceViewHolder(private val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: Device) {
            binding.deviceIdText.text = "ID: ${device.deviceId}"
            binding.deviceNameText.text = device.deviceName
            binding.deviceTypeText.text = device.deviceType
            binding.deviceStatusText.text = device.status
            binding.deviceIpText.text = device.ipAddress
            binding.devicePortText.text = "Port: ${device.port}"
        }
    }
} 
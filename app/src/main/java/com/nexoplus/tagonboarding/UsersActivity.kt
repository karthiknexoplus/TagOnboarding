package com.nexoplus.tagonboarding

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nexoplus.tagonboarding.databinding.ActivityUsersBinding
import com.nexoplus.tagonboarding.databinding.ItemUserBinding
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView as RV
import android.app.AlertDialog
import android.view.View
import android.widget.EditText
import okhttp3.FormBody

class UsersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUsersBinding
    private lateinit var adapter: UsersAdapter
    private var allUsers = mutableListOf<User>()
    private var pagedUsers = mutableListOf<User>()
    private var currentPage = 0
    private val pageSize = 10
    private var isLoading = false
    private val logTag = "NexoplusUsers"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = UsersAdapter(pagedUsers, ::showEditDialog, ::confirmDeleteUser)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1) && !isLoading) {
                    Log.d(logTag, "Scrolled to bottom, loading next page ($currentPage)")
                    loadNextPage()
                }
            }
        })

        binding.filterEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        Log.d(logTag, "Show Users screen opened, loading all users from API")
        loadAllUsers()
    }

    private fun loadAllUsers() {
        isLoading = true
        val client = OkHttpClient()
        val url = "http://136.232.224.78:5000/getuser"
        Log.d(logTag, "Requesting users from: $url")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "curl/7.64.1")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(logTag, "Failed to load users: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@UsersActivity, "Failed to load users: ${e.message}", Toast.LENGTH_LONG).show()
                    isLoading = false
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d(logTag, "Response code: ${response.code}, body: $body")
                runOnUiThread {
                    if (response.isSuccessful && body != null && body.trim().startsWith("[")) {
                        val jsonArray = JSONArray(body)
                        Log.d(logTag, "Parsed ${jsonArray.length()} users from response")
                        allUsers.clear()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            Log.d(logTag, "User $i: $obj")
                            allUsers.add(User(
                                obj.optInt("id"),
                                obj.optString("name"),
                                obj.optString("designation"),
                                obj.optString("vehicle_number"),
                                obj.optString("fastag_id")
                            ))
                        }
                        binding.totalUsersText.text = "Total users: ${allUsers.size}"
                        currentPage = 0
                        pagedUsers.clear()
                        loadNextPage()
                    } else {
                        Log.e(logTag, "Failed to load users, response not JSON array. Body: $body")
                        Toast.makeText(this@UsersActivity, "Failed to load users: Invalid response from server", Toast.LENGTH_LONG).show()
                    }
                    isLoading = false
                }
            }
        })
    }

    private fun loadNextPage() {
        if (allUsers.isEmpty()) return
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, allUsers.size)
        if (start >= allUsers.size) return
        pagedUsers.addAll(allUsers.subList(start, end))
        adapter.notifyDataSetChanged()
        currentPage++
    }

    private fun showEditDialog(user: User, position: Int) {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null)
        val nameInput = EditText(this)
        nameInput.setText(user.name)
        val vehicleInput = EditText(this)
        vehicleInput.setText(user.vehicleNumber)
        val designationInput = EditText(this)
        designationInput.setText(user.designation)
        val fastagInput = EditText(this)
        fastagInput.setText(user.fastagId)
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.addView(nameInput)
        layout.addView(vehicleInput)
        layout.addView(designationInput)
        layout.addView(fastagInput)
        AlertDialog.Builder(this)
            .setTitle("Edit User")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                postEditUser(user, nameInput.text.toString(), vehicleInput.text.toString(), designationInput.text.toString(), fastagInput.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun postEditUser(user: User, name: String, vehicle: String, designation: String, fastag: String) {
        val client = OkHttpClient()
        val url = "http://136.232.224.78:5000/edit_rfid_users"
        val body = FormBody.Builder()
            .add("fastag_id", fastag)
            .add("name", name)
            .add("vehicle_number", vehicle)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@UsersActivity, "Edit failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@UsersActivity, "User updated!", Toast.LENGTH_SHORT).show()
                        loadAllUsers()
                    } else {
                        Toast.makeText(this@UsersActivity, "Edit failed: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun confirmDeleteUser(user: User, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete this user?")
            .setPositiveButton("Delete") { _, _ -> postDeleteUser(user) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun postDeleteUser(user: User) {
        val client = OkHttpClient()
        val url = "http://136.232.224.78:5000/delete_rfid_users"
        val body = FormBody.Builder()
            .add("fastag_id", user.fastagId)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@UsersActivity, "Delete failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@UsersActivity, "User deleted!", Toast.LENGTH_SHORT).show()
                        loadAllUsers()
                    } else {
                        Toast.makeText(this@UsersActivity, "Delete failed: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun filterUsers(query: String) {
        pagedUsers.clear()
        if (query.isBlank()) {
            // Show first page of all users
            val end = minOf(pageSize, allUsers.size)
            pagedUsers.addAll(allUsers.subList(0, end))
            currentPage = 1
        } else {
            val lower = query.lowercase()
            val filtered = allUsers.filter {
                it.name.lowercase().contains(lower) ||
                it.vehicleNumber.lowercase().contains(lower)
            }
            pagedUsers.addAll(filtered)
            currentPage = 0 // Disable paging when filtering
        }
        adapter.notifyDataSetChanged()
        binding.totalUsersText.text = "Total users: ${pagedUsers.size}"
    }
}

// User data class
data class User(val id: Int, val name: String, val designation: String, val vehicleNumber: String, val fastagId: String)

// RecyclerView Adapter
class UsersAdapter(private val users: List<User>, private val onEdit: (User, Int) -> Unit, private val onDelete: (User, Int) -> Unit) : RV.Adapter<UsersAdapter.UserViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
        holder.binding.editIcon.setOnClickListener { onEdit(users[position], position) }
        holder.binding.deleteIcon.setOnClickListener { onDelete(users[position], position) }
    }
    override fun getItemCount() = users.size
    class UserViewHolder(val binding: ItemUserBinding) : RV.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.nameText.text = user.name
            binding.designationText.text = user.designation
            binding.vehicleNumberText.text = user.vehicleNumber
            binding.fastagIdText.text = user.fastagId
        }
    }
} 
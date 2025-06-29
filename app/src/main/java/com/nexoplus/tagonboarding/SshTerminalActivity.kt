package com.nexoplus.tagonboarding

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nexoplus.tagonboarding.databinding.ActivitySshTerminalBinding
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialog

class SshTerminalActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySshTerminalBinding
    private var channel: ChannelShell? = null
    private var inputToServer: OutputStream? = null
    private var outputFromServer: InputStream? = null
    private var session: Session? = null
    private var isConnected = false
    private val storedCommands = listOf(
        "CTRL+C",
        "cd Onebee",
        "tail -f logs/flask.log",
        "tail -f logs/camera_process.log",
        "tail -f logs/rfid_reader1.log",
        "tail -f logs/rfid_reader2.log",
        "tail -f logs/rfid_reader1.log tail -f logs/rfid_reader2.log",
        "sudo supervisorctl restart all",
        "sudo supervisorctl status",
        "CLEAR"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySshTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set default credentials
        binding.ipEditText.setText("136.232.224.78")
        binding.usernameEditText.setText("onebee")
        binding.passwordEditText.setText("onebee")

        binding.connectButton.setOnClickListener {
            if (isConnected) {
                Toast.makeText(this, "Already connected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val ip = binding.ipEditText.text.toString().trim()
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            if (ip.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "IP, username, and password are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.outputTextView.text = "Connecting...\n"
            thread {
                try {
                    val jsch = JSch()
                    session = jsch.getSession(username, ip, 22)
                    session?.setPassword(password)
                    session?.setConfig("StrictHostKeyChecking", "no")
                    session?.connect(10000)
                    channel = session?.openChannel("shell") as ChannelShell
                    channel?.setPty(true)
                    inputToServer = channel?.outputStream
                    outputFromServer = channel?.inputStream
                    channel?.connect()
                    isConnected = true
                    runOnUiThread {
                        binding.outputTextView.append("Connected!\n")
                        scrollToBottom()
                    }
                    val reader = outputFromServer!!.bufferedReader()
                    while (channel?.isConnected == true) {
                        val line = reader.readLine() ?: break
                        runOnUiThread {
                            binding.outputTextView.append(line + "\n")
                            scrollToBottom()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        binding.outputTextView.append("Error: ${e.message}\n")
                        scrollToBottom()
                    }
                } finally {
                    isConnected = false
                    channel?.disconnect()
                    session?.disconnect()
                    runOnUiThread {
                        binding.outputTextView.append("Disconnected.\n")
                        scrollToBottom()
                    }
                }
            }
        }

        binding.disconnectButton.setOnClickListener {
            if (!isConnected) {
                Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            thread {
                try {
                    channel?.disconnect()
                    session?.disconnect()
                } catch (e: Exception) {
                    runOnUiThread {
                        binding.outputTextView.append("Error disconnecting: ${e.message}\n")
                        scrollToBottom()
                    }
                } finally {
                    isConnected = false
                    runOnUiThread {
                        binding.outputTextView.append("Disconnected by user.\n")
                        scrollToBottom()
                    }
                }
            }
        }

        binding.sendButton.setOnClickListener {
            val cmd = binding.inputEditText.text.toString()
            if (cmd.isNotBlank() && isConnected) {
                thread {
                    try {
                        inputToServer?.write((cmd + "\n").toByteArray())
                        inputToServer?.flush()
                        runOnUiThread { binding.inputEditText.text.clear() }
                    } catch (e: Exception) {
                        runOnUiThread {
                            binding.outputTextView.append("Send error: ${e.message}\n")
                            scrollToBottom()
                        }
                    }
                }
            } else if (!isConnected) {
                Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup FAB to show bottom sheet with commands
        binding.commandsFab.setOnClickListener {
            val dialog = BottomSheetDialog(this)
            val sheetLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }
            storedCommands.forEach { cmd ->
                val btn = android.widget.Button(this).apply {
                    text = cmd
                    textSize = 14f
                    setPadding(16, 8, 16, 8)
                    setAllCaps(false)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 4, 0, 4)
                    }
                }
                btn.setOnClickListener {
                    dialog.dismiss()
                    when (cmd) {
                        "CTRL+C" -> {
                            if (isConnected) {
                                thread {
                                    try {
                                        inputToServer?.write(3)
                                        inputToServer?.flush()
                                    } catch (e: Exception) {
                                        runOnUiThread {
                                            binding.outputTextView.append("Send error: ${e.message}\n")
                                            scrollToBottom()
                                        }
                                    }
                                }
                            }
                        }
                        "CLEAR" -> {
                            binding.outputTextView.text = ""
                        }
                        else -> {
                            if (isConnected) {
                                thread {
                                    try {
                                        inputToServer?.write((cmd + "\n").toByteArray())
                                        inputToServer?.flush()
                                    } catch (e: Exception) {
                                        runOnUiThread {
                                            binding.outputTextView.append("Send error: ${e.message}\n")
                                            scrollToBottom()
                                        }
                                    }
                                }
                            } else {
                                binding.inputEditText.setText(cmd)
                            }
                        }
                    }
                }
                sheetLayout.addView(btn)
            }
            dialog.setContentView(sheetLayout)
            dialog.show()
        }
    }

    private fun scrollToBottom() {
        binding.outputScrollView.post {
            binding.outputScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
}

class CommandViewHolder(val button: android.widget.Button) : RecyclerView.ViewHolder(button) 
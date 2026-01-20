package com.example.udpnotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var etPort: EditText
    private lateinit var tvIpAddress: TextView
    private lateinit var tvMessageCount: TextView
    private lateinit var tvLastMessage: TextView
    private lateinit var tvInfo: TextView
    private lateinit var statusIndicator: View
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnSend: Button
    private lateinit var etTargetIp: EditText
    private lateinit var etTargetPort: EditText
    private lateinit var etMessage: EditText

    private var messageCount = 0
    private var isServiceRunning = false

    private val PREFS_NAME = "UdpNotifierPrefs"
    private val PREFS_KEY_PORT = "port"
    private val PREFS_KEY_TARGET_IP = "target_ip"
    private val PREFS_KEY_TARGET_PORT = "target_port"

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "UDP_MESSAGE_RECEIVED" -> {
                    messageCount++
                    updateMessageCount()
                    updateLastMessageTime()
                }
                "UDP_SERVICE_STARTED" -> {
                    isServiceRunning = true
                    updateServiceStatus()
                    Toast.makeText(context, "Servicio de escucha iniciado", Toast.LENGTH_SHORT).show()
                }
                "UDP_SERVICE_STOPPED" -> {
                    isServiceRunning = false
                    updateServiceStatus()
                    Toast.makeText(context, "Servicio de escucha detenido", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadSavedValues()
        setupButtons()
        updateNetworkInfo()
        updateServiceStatus()

        etPort.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateNetworkInfo()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val filter = IntentFilter().apply {
            addAction("UDP_MESSAGE_RECEIVED")
            addAction("UDP_SERVICE_STARTED")
            addAction("UDP_SERVICE_STOPPED")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        etPort = findViewById(R.id.etPort)
        tvIpAddress = findViewById(R.id.tvIpAddress)
        tvMessageCount = findViewById(R.id.tvMessageCount)
        tvLastMessage = findViewById(R.id.tvLastMessage)
        tvInfo = findViewById(R.id.tvInfo)
        statusIndicator = findViewById(R.id.statusIndicator)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnSend = findViewById(R.id.btnSend)
        etTargetIp = findViewById(R.id.etTargetIp)
        etTargetPort = findViewById(R.id.etTargetPort)
        etMessage = findViewById(R.id.etMessage)
    }

    private fun setupButtons() {
        btnStart.setOnClickListener {
            startUdpService()
        }

        btnStop.setOnClickListener {
            stopUdpService()
        }

        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun startUdpService() {
        val portStr = etPort.text.toString()
        if (portStr.isBlank()) {
            Toast.makeText(this, "Por favor, introduce un puerto de escucha", Toast.LENGTH_SHORT).show()
            return
        }
        val port = portStr.toInt()
        savePort(port)

        val intent = Intent(this, UdpService::class.java)
        intent.putExtra("UDP_PORT", port)
        startForegroundService(intent)
    }

    private fun stopUdpService() {
        val intent = Intent(this, UdpService::class.java)
        stopService(intent)
    }

    private fun sendMessage() {
        val targetIp = etTargetIp.text.toString()
        val message = etMessage.text.toString()
        val targetPortStr = etTargetPort.text.toString()

        if (targetIp.isBlank()) {
            Toast.makeText(this, "Por favor, introduce la IP de destino", Toast.LENGTH_SHORT).show()
            return
        }

        if (targetPortStr.isBlank()) {
            Toast.makeText(this, "Por favor, introduce el puerto de destino", Toast.LENGTH_SHORT).show()
            return
        }

        if (message.isBlank()) {
            Toast.makeText(this, "Por favor, escribe un mensaje", Toast.LENGTH_SHORT).show()
            return
        }

        val targetPort = targetPortStr.toInt()
        saveTargetIp(targetIp)
        saveTargetPort(targetPort)

        thread {
            try {
                val socket = DatagramSocket()
                val packet = DatagramPacket(
                    message.toByteArray(),
                    message.length,
                    InetAddress.getByName(targetIp),
                    targetPort
                )
                socket.send(packet)
                socket.close()

                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Mensaje enviado a $targetIp:$targetPort",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Error al enviar mensaje: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateServiceStatus() {
        if (isServiceRunning) {
            tvStatus.text = "Activo - Escuchando"
            tvStatus.setTextColor(Color.parseColor("#4CAF50"))
            statusIndicator.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            btnStart.isEnabled = false
            btnStop.isEnabled = true
            etPort.isEnabled = false
        } else {
            tvStatus.text = "Detenido"
            tvStatus.setTextColor(Color.parseColor("#F44336"))
            statusIndicator.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))
            btnStart.isEnabled = true
            btnStop.isEnabled = false
            etPort.isEnabled = true
        }
    }

    private fun updateNetworkInfo() {
        val ip = getLocalIpAddress()
        tvIpAddress.text = if (ip != null) {
            "IP: $ip"
        } else {
            "IP: No disponible"
        }
        val port = etPort.text.toString()
        tvInfo.text = "💡 Envía mensajes UDP al puerto de escucha de esta IP para recibir notificaciones"
    }

    private fun updateMessageCount() {
        tvMessageCount.text = messageCount.toString()
    }

    private fun updateLastMessageTime() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        tvLastMessage.text = sdf.format(Date())
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun savePort(port: Int) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putInt(PREFS_KEY_PORT, port)
            apply()
        }
    }

    private fun saveTargetIp(ip: String) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(PREFS_KEY_TARGET_IP, ip)
            apply()
        }
    }

    private fun saveTargetPort(port: Int) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putInt(PREFS_KEY_TARGET_PORT, port)
            apply()
        }
    }

    private fun loadSavedValues() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val listenPort = sharedPrefs.getInt(PREFS_KEY_PORT, 9999)
        val targetIp = sharedPrefs.getString(PREFS_KEY_TARGET_IP, "")
        val targetPort = sharedPrefs.getInt(PREFS_KEY_TARGET_PORT, 9999)
        etPort.setText(listenPort.toString())
        etTargetIp.setText(targetIp)
        etTargetPort.setText(targetPort.toString())
    }
}

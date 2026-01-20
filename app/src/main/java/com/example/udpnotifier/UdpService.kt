package com.example.udpnotifier

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

class UdpService : Service() {

    companion object {
        const val CHANNEL_ID = "udp_channel"
        const val NOTIF_ID = 1
    }

    private var udpSocket: DatagramSocket? = null
    private var isRunning = false
    private var listenerThread: Thread? = null
    private var currentPort: Int = -1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d("UdpService", "onCreate")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        currentPort = intent?.getIntExtra("UDP_PORT", 9999) ?: 9999

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("UDP Notifier")
            .setContentText("Escuchando en puerto $currentPort")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }

        Log.d("UdpService", "startForeground OK on port $currentPort")

        // Notificar que el servicio se inició
        sendBroadcast("UDP_SERVICE_STARTED")

        // Iniciar listener UDP
        startUdpListener(currentPort)

        return START_STICKY
    }

    private fun startUdpListener(port: Int) {
        if (isRunning) return

        isRunning = true
        listenerThread = thread {
            try {
                udpSocket = DatagramSocket(port)
                val buffer = ByteArray(1024)

                Log.d("UdpService", "UDP listener iniciado en puerto $port")

                while (isRunning) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.receive(packet)

                    val message = String(packet.data, 0, packet.length)
                    val senderIp = packet.address.hostAddress

                    Log.d("UdpService", "Mensaje recibido de $senderIp: $message")

                    // Notificar que se recibió un mensaje
                    sendBroadcast("UDP_MESSAGE_RECEIVED")

                    // Mostrar notificación con el mensaje
                    showNotification(message, senderIp ?: "Unknown")
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e("UdpService", "Error en UDP listener", e)
                }
            }
        }
    }

    private fun showNotification(message: String, senderIp: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Mensaje UDP de $senderIp")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setAutoCancel(true)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .build()

        val notifId = System.currentTimeMillis().toInt()
        notificationManager.notify(notifId, notification)
    }

    private fun sendBroadcast(action: String) {
        val intent = Intent(action)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        udpSocket?.close()
        listenerThread?.interrupt()

        // Notificar que el servicio se detuvo
        sendBroadcast("UDP_SERVICE_STOPPED")

        Log.d("UdpService", "onDestroy - UDP listener detenido")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "UDP Listener",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de mensajes UDP"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
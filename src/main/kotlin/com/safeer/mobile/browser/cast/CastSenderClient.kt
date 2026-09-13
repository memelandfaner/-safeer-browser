package com.safeer.mobile.browser.cast

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Safeer Cast Sender Client za mobilni brskalnik (safeer-browser).
 *
 * Omogoča:
 * 1. Povezavo s Safeer Cast Hubom
 * 2. Poslušanje seznama aktivnih naprav (TV-ji v omrežju)
 * 3. Pošiljanje spletnih strani in videov na TV (cast.url)
 * 4. Nadzor predvajanja (cast.control: play/pause/seek)
 * 5. Spremljanje stanja predvajalnika (cast.status)
 */
class CastSenderClient(
    private val hubWsUrl: String,
    private val senderId: String = "phone-" + UUID.randomUUID().toString().take(8)
) {
    companion object {
        private const val TAG = "SafeerCastSender"
    }

    data class Device(
        val id: String,
        val name: String,
        val role: String,
        val capabilities: List<String>
    )

    data class PlaybackStatus(
        val deviceId: String,
        val state: String,
        val currentUrl: String?,
        val title: String?,
        val position: Double,
        val duration: Double
    )

    var onDevicesChanged: ((List<Device>) -> Unit)? = null
    var onPlaybackStatus: ((PlaybackStatus) -> Unit)? = null
    var onConnectedStateChanged: ((Boolean) -> Unit)? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isConnected = false

    fun connect() {
        Log.i(TAG, "Povezujem se na Safeer Cast Hub: $hubWsUrl")
        val request = Request.Builder().url(hubWsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.i(TAG, "Povezan na Cast Hub!")
                isConnected = true
                mainHandler.post { onConnectedStateChanged?.invoke(true) }

                // Registracija kot pošiljatelj (Sender)
                val registerMsg = JSONObject().apply {
                    put("id", UUID.randomUUID().toString())
                    put("type", "cast.register")
                    put("payload", JSONObject().apply {
                        put("device_id", senderId)
                        put("name", "Safeer Mobile Phone")
                        put("role", "sender")
                    })
                }
                ws.send(registerMsg.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Povezava s hubom neuspešna: ${t.message}")
                isConnected = false
                mainHandler.post { onConnectedStateChanged?.invoke(false) }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "Povezava zaprta: $reason")
                isConnected = false
                mainHandler.post { onConnectedStateChanged?.invoke(false) }
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")

            when (type) {
                "cast.devices" -> {
                    val devicesArray = json.optJSONArray("devices") ?: JSONArray()
                    val devicesList = mutableListOf<Device>()
                    for (i in 0 until devicesArray.length()) {
                        val d = devicesArray.getJSONObject(i)
                        val caps = mutableListOf<String>()
                        val capsArray = d.optJSONArray("capabilities")
                        if (capsArray != null) {
                            for (j in 0 until capsArray.length()) {
                                caps.add(capsArray.getString(j))
                            }
                        }
                        devicesList.add(
                            Device(
                                id = d.getString("id"),
                                name = d.getString("name"),
                                role = d.optString("role", "receiver"),
                                capabilities = caps
                            )
                        )
                    }
                    mainHandler.post { onDevicesChanged?.invoke(devicesList) }
                }

                "cast.status" -> {
                    val devId = json.getString("device_id")
                    val payload = json.getJSONObject("payload")
                    val status = PlaybackStatus(
                        deviceId = devId,
                        state = payload.optString("state", "idle"),
                        currentUrl = payload.optString("current_url", null),
                        title = payload.optString("title", null),
                        position = payload.optDouble("position", 0.0),
                        duration = payload.optDouble("duration", 0.0)
                    )
                    mainHandler.post { onPlaybackStatus?.invoke(status) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Napaka pri razčlenjevanju sporočila: ${e.message}")
        }
    }

    fun sendUrl(targetDeviceId: String, url: String, title: String? = null, startPosition: Double = 0.0) {
        val msg = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("type", "cast.url")
            put("target", targetDeviceId)
            put("payload", JSONObject().apply {
                put("url", url)
                if (title != null) put("title", title)
                put("start_position", startPosition)
            })
        }
        webSocket?.send(msg.toString())
    }

    fun sendControl(targetDeviceId: String, action: String, position: Double? = null, volume: Double? = null) {
        val msg = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("type", "cast.control")
            put("target", targetDeviceId)
            put("payload", JSONObject().apply {
                put("action", action)
                if (position != null) put("position", position)
                if (volume != null) put("volume", volume)
            })
        }
        webSocket?.send(msg.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
    }
}

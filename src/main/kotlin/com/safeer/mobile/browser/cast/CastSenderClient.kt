package com.safeer.mobile.browser.cast

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
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
    private val controlToken: String? = null,
    private val ticketPath: String = "/cast/ticket",
    private val senderId: String = "phone-" + UUID.randomUUID().toString().take(8),
    /**
     * Ali ta naprava sinhronizira. Zmoznost "sync" prijavimo Hubu samo, kadar je
     * vklopljena -- Hub sync sporocila poslje le napravam, ki jo prijavijo, zato
     * izklopljena naprava tujih zaznamkov niti ne prejme niti jih ne oddaja.
     */
    private val sinhronizira: Boolean = false
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

    /** Prejeti podatki sinhronizacije: kategorija, razlicica, casovni zig in vsebina. */
    var onSyncData: ((String, Long, Double, JSONObject) -> Unit)? = null

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
        zVstopnico(hubWsUrl, controlToken) { naslov -> odpriPovezavo(naslov) }
    }

    private fun odpriPovezavo(naslov: String) {
        val request = Request.Builder().url(naslov).build()

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
                        if (sinhronizira) {
                            put("capabilities", JSONArray().put("sync"))
                        }
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


    /**
     * Vzame enokratno vstopnico pri Safeer Controlu in sele nato odpre WebSocket.
     *
     * Vstopnica velja 30 sekund in se porabi ob prvi uporabi, zato jo vzamemo pri vsaki
     * povezavi posebej. Ce zetona ni, se povezemo brez nje (staro vozlisce) in to povemo
     * v dnevniku -- nezasciteno pot pustimo vidno, ne tiho.
     */
    private fun zVstopnico(wsUrl: String, token: String?, naprej: (String) -> Unit) {
        if (token.isNullOrBlank()) {
            Log.w(TAG, "Zeton za Safeer Control ni nastavljen - povezujem se BREZ avtentikacije.")
            naprej(wsUrl)
            return
        }
        val osnova = wsUrl.replace(Regex("^wss"), "https").replace(Regex("^ws"), "http")
            .substringBefore("/cast/ws").substringBefore("/link/ws").substringBefore("/safeer/ws")
            .trimEnd('/')
        val zahteva = Request.Builder()
            .url("$osnova${ticketPath()}")
            .addHeader("X-Safeer-Token", token)
            .post("".toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(zahteva).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.w(TAG, "Vstopnice ni bilo mogoce dobiti: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val telo = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        Log.w(TAG, "Control je zavrnil zahtevo za vstopnico (${it.code}).")
                        return
                    }
                    val vstopnica = try {
                        JSONObject(telo).optString("ticket")
                    } catch (e: Exception) {
                        ""
                    }
                    if (vstopnica.isBlank()) {
                        Log.w(TAG, "Odgovor Controla ne vsebuje vstopnice.")
                        return
                    }
                    val locilo = if (wsUrl.contains("?")) "&" else "?"
                    naprej("$wsUrl${locilo}ticket=$vstopnica")
                }
            }
        })
    }


    private fun ticketPath(): String = ticketPath

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

                "sync.data" -> {
                    val payload = json.optJSONObject("payload") ?: JSONObject()
                    val kategorija = payload.optString("category", "")
                    val razlicica = payload.optLong("version", 0L)
                    val zig = payload.optDouble("timestamp", 0.0)
                    val vsebina = payload.optJSONObject("data") ?: JSONObject()
                    if (kategorija.isNotEmpty()) {
                        mainHandler.post { onSyncData?.invoke(kategorija, razlicica, zig, vsebina) }
                    }
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

    /**
     * Poslje svoje stanje kategorije vsem, ki sinhronizirajo, in Hubu, ki ga shrani.
     * Vsebina je poljuben JSON; pomen pozna samo naprava, Hub ga ne odpira.
     */
    fun posljiSinhronizacijo(kategorija: String, razlicica: Long, vsebina: JSONObject) {
        val msg = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("type", "sync.data")
            put("target", "all")
            put("payload", JSONObject().apply {
                put("category", kategorija)
                put("version", razlicica)
                put("timestamp", System.currentTimeMillis() / 1000.0)
                put("data", vsebina)
            })
        }
        webSocket?.send(msg.toString())
    }

    /** Vprasa Hub za zadnje znano stanje kategorije (npr. ob prvem vklopu). */
    fun zahtevajSinhronizacijo(kategorija: String, odRazlicice: Long? = null) {
        val msg = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("type", "sync.request")
            put("payload", JSONObject().apply {
                put("category", kategorija)
                if (odRazlicice != null) put("since_version", odRazlicice)
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

package com.safeer.mobile.browser

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.Proxy
import java.util.concurrent.TimeUnit

/** Quad9 requires HTTP/2, which Android HttpURLConnection cannot negotiate. */
object Http2DnsTransport {
    private val client = okhttp3.OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    fun query(endpoint: String, wireQuery: ByteArray, timeoutMs: Int): ByteArray? {
        if (!endpoint.startsWith("https://", true)) return null
        return try {
            val request = Request.Builder().url(endpoint)
                .header("Accept", "application/dns-message")
                .post(wireQuery.toRequestBody("application/dns-message".toMediaType())).build()
            val bounded = client.newBuilder().connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .callTimeout(timeoutMs.toLong() * 2, TimeUnit.MILLISECONDS).build()
            bounded.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body ?: return null
                if (body.contentLength() > 65535) return null
                body.byteStream().use { input ->
                    val bytes = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (bytes.size() + count > 65535) return null
                        bytes.write(buffer, 0, count)
                    }
                    bytes.toByteArray()
                }
            }
        } catch (_: Exception) { null }
    }
}

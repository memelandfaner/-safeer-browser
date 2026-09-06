package com.safeer.mobile.browser

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Loopback-only HTTP proxy. TLS stays end-to-end inside CONNECT tunnels. */
open class LocalDnsProxy(private val resolveHost: (String) -> String?) {
    private var listener: ServerSocket? = null
    var actualPort: Int = 0
        private set
    val active = AtomicBoolean(false)
    private val sockets = ConcurrentHashMap.newKeySet<Socket>()
    private val workers = ThreadPoolExecutor(0, 64, 30L, TimeUnit.SECONDS,
        SynchronousQueue<Runnable>(), { r -> Thread(r, "SafeerProxy").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy())

    @Synchronized fun start(): Int {
        if (active.get()) return actualPort
        check(!workers.isShutdown) { "Create a new proxy after stop" }
        listener = ServerSocket(0, 64, InetAddress.getByName("127.0.0.1"))
        actualPort = listener!!.localPort
        active.set(true)
        Thread({
            while (active.get()) {
                val client = try { listener?.accept() ?: break } catch (_: Exception) { break }
                sockets.add(client)
                if (!active.get()) { client.close(); sockets.remove(client); break }
                try { workers.execute { handle(client) } }
                catch (_: java.util.concurrent.RejectedExecutionException) {
                    // Never run a blocking tunnel on the acceptor or another tunnel's thread.
                    client.close()
                    sockets.remove(client)
                }
            }
        }, "SafeerProxyAcceptor").apply { isDaemon = true }.start()
        return actualPort
    }

    @Synchronized fun stop() {
        active.set(false)
        try { listener?.close() } catch (_: Exception) {}
        listener = null
        sockets.forEach { try { it.close() } catch (_: Exception) {} }
        workers.shutdownNow()
    }

    private fun readHeader(input: InputStream): String {
        val bytes = ByteArrayOutputStream()
        var tail = 0
        while (bytes.size() < 16384) {
            val value = input.read()
            require(value >= 0) { "Incomplete proxy request" }
            bytes.write(value)
            tail = (tail shl 8) or value
            if (tail == 0x0d0a0d0a) return bytes.toString("ISO-8859-1")
        }
        throw IllegalArgumentException("Proxy header too large")
    }

    private fun handle(client: Socket) {
        var remote: Socket? = null
        var connected = false
        try {
            client.soTimeout = 15000
            val input = BufferedInputStream(client.getInputStream())
            val output = client.getOutputStream()
            val header = readHeader(input)
            val request = header.substringBefore("\r\n").split(' ')
            require(request.size == 3)
            val connect = request[0] == "CONNECT"
            val target = URI(if (connect) "http://${request[1]}" else request[1])
            require(target.scheme == "http" && target.userInfo == null && target.fragment == null)
            val host = requireNotNull(target.host).removeSurrounding("[", "]")
            val port = if (target.port == -1) { if (connect) 443 else 80 } else target.port
            require(port in 1..65535)
            val ip = requireNotNull(resolveHost(host)) { "DNS lookup failed" }
            require(!(InetAddress.getByName(ip).isLoopbackAddress && port == actualPort))
            val upstream = Socket()
            remote = upstream
            sockets.add(upstream)
            check(active.get())
            upstream.connect(InetSocketAddress(ip, port), 10000)
            upstream.soTimeout = 120000
            client.soTimeout = 120000
            if (connect) {
                output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
                output.flush()
            } else {
                val path = (target.rawPath?.takeIf { it.isNotEmpty() } ?: "/") +
                    (target.rawQuery?.let { "?$it" } ?: "")
                val fields = header.split("\r\n").drop(1).filter {
                    it.isNotEmpty() && !it.startsWith("Proxy-", true) &&
                        !it.startsWith("Connection:", true)
                }
                // One origin per plain HTTP connection; body bytes remain in the buffered input.
                val outgoing = "${request[0]} $path ${request[2]}\r\n" +
                    fields.joinToString("\r\n") + "\r\nConnection: close\r\n\r\n"
                upstream.getOutputStream().write(outgoing.toByteArray(Charsets.ISO_8859_1))
            }
            connected = true
            // Dedicated reverse pump prevents pool starvation with concurrent CONNECT requests.
            val upload = Thread({
                try { copy(input, upstream.getOutputStream()) } catch (_: Exception) {}
                finally { try { upstream.shutdownOutput() } catch (_: Exception) {} }
            }, "SafeerProxyUpload").apply { isDaemon = true; start() }
            try { copy(upstream.getInputStream(), output) }
            finally {
                client.close()
                upstream.close()
                upload.join(1000)
            }
        } catch (_: Exception) {
            if (!connected) try {
                client.getOutputStream().write(("HTTP/1.1 502 Bad Gateway\r\n" +
                    "Content-Length: 0\r\nConnection: close\r\n\r\n").toByteArray(Charsets.ISO_8859_1))
            } catch (_: Exception) {}
        } finally {
            try { client.close() } catch (_: Exception) {}
            try { remote?.close() } catch (_: Exception) {}
            sockets.remove(client)
            remote?.let { sockets.remove(it) }
        }
    }

    private fun copy(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(16384)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) return
            output.write(buffer, 0, count)
            output.flush()
        }
    }
}

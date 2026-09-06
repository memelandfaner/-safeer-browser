package com.safeer.mobile.browser

import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private fun header(socket: Socket): String {
    val result = StringBuilder()
    while (!result.endsWith("\r\n\r\n")) {
        val b = socket.getInputStream().read()
        check(b >= 0)
        result.append(b.toChar())
    }
    return result.toString()
}

fun main() {
    val origin = ServerSocket(0)
    val echoWorkers = Executors.newCachedThreadPool()
    val acceptor = Thread {
        try { while (!origin.isClosed) {
            val s = origin.accept()
            echoWorkers.execute {
                s.use {
                    val input = s.getInputStream()
                    val output = s.getOutputStream()
                    val buf = ByteArray(4096)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n); output.flush()
                    }
                }
            }
        } } catch (_: Exception) {}
    }.apply { start() }
    val proxy = LocalDnsProxy { if (it == "echo.test") "127.0.0.1" else null }
    val port = proxy.start()
    val clients = Executors.newFixedThreadPool(40)
    val connected = CountDownLatch(40)
    val release = CountDownLatch(1)
    try {
        val jobs = (1..40).map { n -> clients.submit {
            Socket("127.0.0.1", port).use { s ->
                s.soTimeout = 10000
                val payload = "payload-$n"
                // Coalesced CONNECT + first data must not discard initial TLS-like bytes.
                s.getOutputStream().write(("CONNECT echo.test:${origin.localPort} HTTP/1.1\r\nHost: echo.test\r\n\r\n" + payload).toByteArray())
                check(header(s).startsWith("HTTP/1.1 200"))
                val bytes = ByteArray(payload.length)
                java.io.DataInputStream(s.getInputStream()).readFully(bytes)
                check(String(bytes) == payload)
                connected.countDown()
                check(release.await(12, TimeUnit.SECONDS))
            }
        } }
        check(connected.await(12, TimeUnit.SECONDS)) { "Concurrent tunnels stalled: ${connected.count}" }
        release.countDown()
        jobs.forEach { it.get(15, TimeUnit.SECONDS) }
        println("PASS: 40 simultaneous tunnels, coalesced headers/data, exact payload")
        Socket("127.0.0.1", port).use { s ->
            s.soTimeout = 3000
            s.getOutputStream().write("CONNECT missing.test:443 HTTP/1.1\r\n\r\n".toByteArray())
            check(header(s).startsWith("HTTP/1.1 502"))
        }
        println("PASS: failed DNS does not silently bypass resolver")
        Socket("127.0.0.1", port).use { s ->
            s.soTimeout = 3000
            s.getOutputStream().write(("POST http://echo.test:${origin.localPort}/form?q=1 HTTP/1.1\r\n" +
                "Host: echo.test\r\nContent-Length: 4\r\nProxy-Authorization: private\r\n\r\nDATA").toByteArray())
            val h = header(s)
            check(h.startsWith("POST /form?q=1 HTTP/1.1"))
            check(!h.contains("Proxy-Authorization"))
            val body = ByteArray(4); java.io.DataInputStream(s.getInputStream()).readFully(body)
            check(String(body) == "DATA")
        }
        println("PASS: HTTP POST body, origin-form target, no proxy credentials sent to origin")
        val idle = Socket("127.0.0.1", port)
        idle.soTimeout = 3000
        idle.getOutputStream().write("CONNECT echo.test:${origin.localPort} HTTP/1.1\r\n\r\n".toByteArray())
        check(header(idle).startsWith("HTTP/1.1 200"))
        proxy.stop()
        check(idle.getInputStream().read() == -1)
        idle.close()
        println("PASS: stop closes existing tunnels")
    } finally {
        release.countDown(); proxy.stop(); origin.close(); clients.shutdownNow(); echoWorkers.shutdownNow(); acceptor.join(1000)
    }
}

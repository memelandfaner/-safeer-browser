package com.safeer.mobile.browser
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
fun main() {
 val calls = AtomicInteger()
 var blocked = false
 val resolver = DoHProxyEngine.DoHResolver("https://dns.example/dns-query") { endpoint, query, _ ->
  check(endpoint.startsWith("https://"));check(query.size > 12); calls.incrementAndGet()
  if (blocked) byteArrayOf(0,1,0x81.toByte(),0x83.toByte(),0,0,0,0,0,0,0,0)
  else byteArrayOf(0,1,0x81.toByte(),0x80.toByte(),0,0,0,1,0,0,0,0,
       0,0,1,0,1,0,0,0,60,0,4,93,184.toByte(),216.toByte(),34)
 }
 val pool = Executors.newFixedThreadPool(12)
 try {
  val results = (1..12).map { pool.submit<String?> { resolver.resolve("example.com") } }
  results.forEach { check(it.get() == "93.184.216.34") }
  check(calls.get() == 1)
  blocked = true
  resolver.providerUrl = "https://new.example/dns-query"
  check(resolver.resolve("example.com") == null)
  check(resolver.resolve("example.com") == null)
  check(calls.get() == 2)
  resolver.providerUrl = "http://insecure.example/dns-query"
  check(resolver.resolve("another.example") == null)
  check(calls.get() == 2)
  val cnameResponse = intArrayOf(0,1,129,128,0,1,0,3,0,0,0,0,3,119,119,119,3,98,98,99,3,99,111,109,0,0,1,0,1,192,12,0,5,0,1,0,0,111,178,0,18,3,119,119,119,3,98,98,99,3,99,111,109,3,112,114,105,192,16,192,41,0,5,0,1,0,0,0,94,0,20,3,98,98,99,3,109,97,112,6,102,97,115,116,108,121,3,110,101,116,0,192,71,0,1,0,1,0,0,0,59,0,4,151,101,0,81).map { it.toByte() }.toByteArray()
  val cnameResolver = DoHProxyEngine.DoHResolver("https://dns.example/dns-query") { _, _, _ -> cnameResponse }
  check(cnameResolver.resolve("www.bbc.com") == "151.101.0.81")
  println("PASS: coalesces 12 DNS lookups, clears changed-provider cache, honors negative answers, rejects unencrypted DoH")
 } finally { pool.shutdownNow() }
}

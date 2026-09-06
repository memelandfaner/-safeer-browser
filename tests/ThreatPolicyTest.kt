package com.safeer.mobile.browser
fun main() {
    check(!ThreatBlockEngine.isThreat("https://www.bbc.com/news"))
    check(!ThreatBlockEngine.isThreat("https://www.rtvslo.si/"))
    ThreatBlockEngine.addThreat("compromised.fastly.net", "Malware", "test fixture")
    check(ThreatBlockEngine.isThreat("https://compromised.fastly.net/embed/video.m3u8"))
    ThreatBlockEngine.allowForSession("compromised.fastly.net")
    check(ThreatBlockEngine.isThreat("https://compromised.fastly.net/file"))
    check(!ThreatBlockEngine.isThreat("https://unrelated.fastly.net/file"))
    check(!ThreatBlockEngine.isThreat("https://compromised.fastly.net.example.org/file"))
    val token = ThreatBlockEngine.createBypassToken("warning.test", "https://warning.test/")
    check(ThreatBlockEngine.consumeBypassToken(token) != null)
    check(ThreatBlockEngine.consumeBypassToken(token) == null)
    println("PASS: ordinary sites, CDN malware, no critical bypass, domain boundary, single-use tokens")
}

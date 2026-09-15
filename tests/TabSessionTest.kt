package com.safeer.mobile.browser

fun main() {
    val tabs = listOf(SavedTab("first", "https://example.org/a?q=šč&lang=sl", "Slovenščina", false),
        SavedTab("second", "https://example.net/", "Desktop", true))
    val session = TabSession(tabs, "second")
    check(TabSessionCodec.decode(TabSessionCodec.encode(session)) == session)
    check(TabSessionCodec.decode("not a session").tabs.isEmpty())
    val encoded = TabSessionCodec.encode(session)
    check(TabSessionCodec.decode(encoded.dropLast(8)).tabs.isEmpty())
    for (url in listOf("javascript:alert(1)", "file:///sdcard/private.html", "intent://app", "data:text/html,test", "https://user:pass@example.com/", "https://")) {
        check(TabSessionCodec.safeUrl(url) == TabSessionCodec.HOME) { url }
    }
    val many = (0..249).map { SavedTab("$it", "https://example.org/$it", "Page $it", false) }
    val bounded = TabSessionCodec.decode(TabSessionCodec.encode(TabSession(many, "249")))
    check(bounded.tabs.size == 200 && bounded.tabs.any { it.id == "249" } && bounded.activeId == "249")
    check(TabSessionCodec.decode(TabSessionCodec.encode(TabSession(tabs + tabs.first(), "first"))).tabs.isEmpty())
    println("PASS: session round trip, active/desktop/order, malformed input, unsafe schemes and bounded persistence")
}

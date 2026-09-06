package com.safeer.mobile.browser

fun main() {
    val protected = listOf(
        "https://example.org/callback?utm_source=mail&code=a%2Bb&state=x+y#done",
        "https://example.org/file?utm_source=mail&X-Amz-Signature=abc",
        "https://example.org/file?fbclid=test&%73tate=abc",
        "https://example.org/login?utm_campaign=test&next=%2Fhome",
        "https://example.org/file?gclid=test&sig=abc",
        "https://example.org/file?utm_source=test&redirect_uri=https%3A%2F%2Fexample.org"
    )
    protected.forEach { check(UrlSanitizer.sanitize(it) == it) { "Modified protected link: $it" } }
    check(UrlSanitizer.sanitize("https://example.org/news?q=a%2Bb+word&utm_source=test&x=1&x=2#part") ==
        "https://example.org/news?q=a%2Bb+word&x=1&x=2#part")
    check(UrlSanitizer.sanitize("https://example.org/?fbclid=test#part") == "https://example.org/#part")
    check(UrlSanitizer.sanitize("https://example.org/?utm_source=test&bad=%ZZ") == "https://example.org/?bad=%ZZ")
    check(UrlSanitizer.sanitize("about:srcdoc") == "about:srcdoc")
    listOf("https://chatgpt.com/", "https://accounts.google.com/v3/signin", "https://example.org/login",
        "https://tenant.auth0.com/", "https://challenges.cloudflare.com/").forEach {
        check(AuthenticationPages.isAuthenticationPage(it)) { "Unprotected login page: $it" }
    }
    listOf("https://accounts.google.com.example.org/", "https://notauth0.com/", "https://example.org/blog/login-help",
        "https://www.youtube.com/", "https://www.bbc.com/news").forEach {
        check(!AuthenticationPages.isAuthenticationPage(it)) { "Incorrect login classification: $it" }
    }
    check(!AuthenticationPages.isAuthenticationPage(null))
    println("PASS: signed/auth URLs intact, ordinary trackers removed, raw encoding preserved, authentication domain boundaries")
}

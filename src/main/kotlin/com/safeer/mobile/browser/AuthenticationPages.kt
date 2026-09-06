package com.safeer.mobile.browser

object AuthenticationPages {
    private val hosts = setOf("accounts.google.com", "auth.openai.com", "auth0.openai.com", "chatgpt.com",
        "chat.openai.com", "login.microsoftonline.com", "login.live.com", "appleid.apple.com",
        "challenges.cloudflare.com", "hcaptcha.com", "www.recaptcha.net")
    private val paths = setOf("login", "signin", "sign-in", "oauth", "oauth2", "auth", "authorize")

    fun isAuthenticationPage(url: String?): Boolean = try {
        val uri = java.net.URI(url.orEmpty())
        val host = uri.host.orEmpty().lowercase()
        host in hosts || host.endsWith(".auth0.com") || host.endsWith(".hcaptcha.com") ||
            (host == "www.google.com" && uri.path.orEmpty().startsWith("/recaptcha/")) ||
            uri.path.orEmpty().lowercase().split('/').any { it in paths }
    } catch (_: Exception) { false }
}

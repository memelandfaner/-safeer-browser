package com.safeer.mobile.browser

object AuthenticationPages {
    private val hosts = setOf(
        "accounts.google.com", "auth.openai.com", "auth0.openai.com", "chatgpt.com",
        "chat.openai.com", "login.microsoftonline.com", "login.live.com", "appleid.apple.com",
        "challenges.cloudflare.com", "cloudflare.com", "static.cloudflareinsights.com", "turnstile.com",
        "accounts.x.ai", "auth.x.ai", "x.ai", "grok.com", "www.grok.com",
        "hcaptcha.com", "www.recaptcha.net"
    )
    private val paths = setOf("login", "signin", "sign-in", "oauth", "oauth2", "auth", "authorize", "check-login")

    fun isAuthenticationPage(url: String?): Boolean = try {
        if (url.isNullOrEmpty()) false
        else {
            val uri = java.net.URI(url)
            val host = uri.host.orEmpty().lowercase()
            val path = uri.path.orEmpty().lowercase()
            host in hosts ||
                host.endsWith(".x.ai") || host.endsWith(".grok.com") ||
                host.endsWith(".cloudflare.com") || host.endsWith(".turnstile.com") ||
                host.endsWith(".auth0.com") || host.endsWith(".hcaptcha.com") ||
                (host == "www.google.com" && path.startsWith("/recaptcha/")) ||
                path.startsWith("/cdn-cgi/") || path.contains("challenge-platform") ||
                path.split('/').any { it in paths }
        }
    } catch (_: Exception) { false }
}

package com.safeer.mobile.browser
import android.content.Context
object PreferencesManager {
 fun getSecureProxyMode(c: Context) = "disabled"
 fun isDohEnabled(c: Context) = true
 fun getDohProvider(c: Context) = "quad9"
 fun getSecureProxyUrl(c: Context) = ""
 fun getCustomDohUrl(c: Context) = ""
}

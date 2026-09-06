package android.webkit
class WebResourceResponse(val mime: String, val encoding: String, val status: Int,
    val reason: String, val headers: Map<String,String>, val data: java.io.InputStream)

# AndroidX dependencies

The manual Android build includes these unmodified classes.jar files from Google's Maven repository (https://dl.google.com/dl/android/maven2/):

- androidx.webkit:webkit:1.12.1 (AAR classes.jar)
- androidx.core:core:1.1.0 (AAR classes.jar; WebKit dependency)
- androidx.annotation:annotation-jvm:1.8.1
- androidx.annotation:annotation-experimental:1.4.1 (AAR classes.jar)

AndroidX is licensed under Apache License 2.0. Each JAR retains its packaged license metadata. Checksums of extracted JARs are in SHA256SUMS.

WebKit supplies the supported per-app ProxyController and algorithmic darkening APIs. A protected PROXY_CHANGE broadcast cannot be used by an ordinary Android application.

API documentation: https://developer.android.com/reference/androidx/webkit/ProxyController

These versions are pinned for compatibility with this project's standalone Kotlin compiler. Update the checksums and verify the phone build when upgrading.

HTTP/2 DNS transport dependencies (unmodified JARs from https://repo.maven.apache.org/maven2/):

- com.squareup.okhttp3:okhttp:4.12.0
- com.squareup.okio:okio-jvm:3.6.0

Both are Apache License 2.0. They use the Kotlin standard library already supplied by the build toolchain. Quad9 rejects Android HttpURLConnection (HTTP/1.1) with HTTP 505; OkHttp negotiates HTTP/2 without disabling certificate validation.

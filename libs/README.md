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

Ed25519 signature verification for the signed Safeer Threat Intelligence feed:

- bcprov-ed25519-1.78.1.jar: Bouncy Castle 1.78.1 (org.bouncycastle:bcprov-jdk18on, MIT license, text inside the JAR) reduced by R8 tree shaking to `org.bouncycastle.math.ec.rfc8032.Ed25519.verify` and the classes it needs (45 classes, about 50 KB instead of 8 MB). No optimization or obfuscation passes. Input JAR SHA-256 add5915e6acfc6ab5836e1fd8a5e21c6488536a8c1f21f386eeb3bf280b702d7, JAR-signed by "Legion of the Bouncy Castle Inc.". The build is reproducible with `clients/kotlin/bouncycastle/shrink.sh` in https://github.com/memelandfaner/safeer-threat-intel (R8 8.2.42).

java.security Ed25519 exists only on Android 13+, while this app supports Android 9+. The signed feed tests (tests/SignedThreatFeedTest.kt) run against this exact JAR, including RFC 8032 vectors, a cross-check with the JDK Ed25519 implementation on random and mutated signatures, and the shared conformance corpus.

# Safeer Mobile 1.0.5

Fixes broken page loading caused by the DNS proxy. Quad9 requires HTTP/2, whereas Android HttpURLConnection sent HTTP/1.1 and received HTTP 505. The DNS transport now uses OkHttp with HTTP/2 and normal certificate validation. AndroidX ProxyController replaces the protected PROXY_CHANGE broadcast, and initial navigation waits for the proxy configuration callback.

The loopback proxy now preserves buffered request data, handles concurrent tunnels without thread-pool starvation, limits connection/header waits, and closes sockets when stopped. DNS requests bypass their own local proxy. Negative DNS responses are not silently retried through unencrypted system DNS. New installations default to the device DNS; encrypted DNS remains optional.

Critical malware/C2 matches take priority over compatibility exceptions. Blocked subframes cannot replace the main page. Certificate errors still cancel the connection. Retry and reload retain the failed address. Native WebView darkening replaces a forced page-wide color-scheme override.

Verification: signed APK build, JVM regression suite, HTTP/2 DNS probe on Samsung SM-S931B, successful BBC/RTV rendering, and live checks documented in tests/. Existing application data is preserved by the upgrade. This release does not constitute an exhaustive security audit or a test of every website and media provider.

Cosmetic filtering now collapses explicitly marked ad slots and common reserved ad wrappers, including BBC data-component/data-testid slots. Their padding and margins are removed too. CSS also covers slots inserted after page load; arbitrary empty content containers are not removed. Unrecognized site-specific ad wrappers may still need additional rules.

YouTube checks on Samsung S25: home/search thumbnails, Faded and Alone mix transition, Get Lucky playback, persistent pause/resume and fullscreen/back. No visible ads or player errors during this sample. Removed a failing preconnect to bare googlevideo.com (certificate hostname mismatch) and limited the YouTube supervisor to exact YouTube host boundaries.

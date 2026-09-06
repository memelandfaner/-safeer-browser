# Safeer Mobile regression checks

Run `./tests/run_tests.sh` from the project. The transport tests use local sockets; they need permission to bind loopback ports. Set KOTLINC and ANDROID_JAR to override the local Android toolchain paths.

The tests exercise:

- Forty simultaneous CONNECT tunnels with immediate payload, without starvation or discarded TLS bytes.
- HTTP request bodies and conversion to origin-form, without leaking proxy credentials.
- Shutdown cleanup and failed DNS without a direct-DNS bypass.
- Malware matching on normally exempt CDN domains and single-use exception tokens.
- Coalesced DNS requests, negative caching, provider changes, rejection of cleartext DNS endpoints, and a real BBC CNAME response.

Android classes in `stubs/` and `dns-stubs/` support pure JVM tests only. They are not packaged into the application. TLS validation and HTTP/2 negotiation were also tested on the Samsung SM-S931B using the production networking libraries: Quad9 returned HTTP 505 through HttpURLConnection and HTTP 200 with protocol h2 through OkHttp.

The HTTP records in `evidence/` are uncached client requests through the actual phone's localhost proxy. Their times measure those requests, not full-page rendering. Browser rendering was separately checked on the phone.

Phone UI checks passed: BBC and RTV with Quad9 enabled, Google search, Wikipedia, GitHub, 24ur, opening/closing a test tab, blocking the self-signed.badssl.com certificate, and both Retry and toolbar Reload requesting the original .invalid test address. Temporary diagnostics were removed from the phone.

BBC cosmetic follow-up: installed signed APK on Samsung S25 and visually confirmed the reserved banner above the BBC header disappears while the header, story image/text and cookie controls remain visible. Screenshot: `evidence/bbc-collapsed-ad-slots-1.0.5.png`. General rules cover explicitly marked ad wrappers; this does not establish coverage of every website.

YouTube live checks: Faded → Alone mix, persistent pause at 0:34, resume, fullscreen/back, search for Get Lucky and active unmuted AAudio playback. Get Lucky content was visible 2.48 seconds after its result was clicked (one sample, not a benchmark). Removed failing bare-googlevideo.com preconnect after independently reproducing certificate hostname mismatch. Final APK replay checked after reinstall. No visible ads or player errors observed in the sampled tracks; this does not guarantee future YouTube ad coverage.

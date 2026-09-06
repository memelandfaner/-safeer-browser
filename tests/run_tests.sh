#!/usr/bin/env bash
set -euo pipefail
TEST_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$TEST_DIR")"
SOURCE_DIR="$PROJECT_DIR/src/main/kotlin/com/safeer/mobile/browser"
KOTLINC="${KOTLINC:-$PROJECT_DIR/../streamN-TV2/android_tv/.tools/kotlinc/bin/kotlinc}"
TEST_OUTPUT="$(mktemp -d /tmp/safeer-tests.XXXXXX)"
trap 'rm -rf "$TEST_OUTPUT"' EXIT
"$KOTLINC" "$SOURCE_DIR/LocalDnsProxy.kt" "$TEST_DIR/LocalDnsProxyTest.kt" -include-runtime -d "$TEST_OUTPUT/proxy.jar"
java -jar "$TEST_OUTPUT/proxy.jar"
"$KOTLINC" "$TEST_DIR"/stubs/*.kt "$SOURCE_DIR/DomainSuffixTrie.kt" "$SOURCE_DIR/ThreatBlockEngine.kt" "$TEST_DIR/ThreatPolicyTest.kt" -include-runtime -d "$TEST_OUTPUT/threat.jar"
java -jar "$TEST_OUTPUT/threat.jar"
ANDROID_JAR="${ANDROID_JAR:-$PROJECT_DIR/../streamN-TV2/android_tv/.tools/android.jar}"
DNS_CLASSPATH="$ANDROID_JAR"
for lib in "$PROJECT_DIR"/libs/*.jar; do DNS_CLASSPATH="$DNS_CLASSPATH:$lib"; done
"$KOTLINC" -cp "$DNS_CLASSPATH" "$SOURCE_DIR/DoHProxyEngine.kt" "$SOURCE_DIR/LocalDnsProxy.kt" "$SOURCE_DIR/Http2DnsTransport.kt" "$TEST_DIR"/dns-stubs/*.kt "$TEST_DIR/DoHResolverTest.kt" -include-runtime -d "$TEST_OUTPUT/dns.jar"
java -cp "$TEST_OUTPUT/dns.jar:$DNS_CLASSPATH" com.safeer.mobile.browser.DoHResolverTestKt

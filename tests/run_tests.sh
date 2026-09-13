#!/usr/bin/env bash
set -euo pipefail
TEST_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$TEST_DIR")"
SOURCE_DIR="$PROJECT_DIR/src/main/kotlin/com/safeer/mobile/browser"
KOTLINC="${KOTLINC:-$PROJECT_DIR/../streamN-TV2/android_tv/.tools/kotlinc/bin/kotlinc}"
TEST_OUTPUT="$(mktemp -d /tmp/safeer-tests.XXXXXX)"
trap 'rm -rf "$TEST_OUTPUT"' EXIT
"$KOTLINC" "$SOURCE_DIR/UrlSanitizer.kt" "$SOURCE_DIR/AuthenticationPages.kt" "$TEST_DIR/LoginPolicyTest.kt" -include-runtime -d "$TEST_OUTPUT/login.jar"
java -jar "$TEST_OUTPUT/login.jar"
"$KOTLINC" "$SOURCE_DIR/LocalDnsProxy.kt" "$TEST_DIR/LocalDnsProxyTest.kt" -include-runtime -d "$TEST_OUTPUT/proxy.jar"
java -jar "$TEST_OUTPUT/proxy.jar"
FEED_DIR="$PROJECT_DIR/src/main/kotlin/com/safeer/threatfeed"
"$KOTLINC" -cp "$PROJECT_DIR/libs/bcprov-ed25519-1.78.1.jar" "$TEST_DIR"/stubs/*.kt "$SOURCE_DIR/DomainSuffixTrie.kt" "$SOURCE_DIR/ThreatBlockEngine.kt" "$SOURCE_DIR/AdBlockEngine.kt" "$SOURCE_DIR/SignedThreatIntel.kt" "$FEED_DIR/SignedThreatFeed.kt" "$FEED_DIR/BankGuard.kt" "$FEED_DIR/BankGuardData.kt" "$FEED_DIR/ThreatListAgent.kt" "$FEED_DIR/FilterListEngine.kt" "$TEST_DIR/ThreatPolicyTest.kt" -include-runtime -d "$TEST_OUTPUT/threat.jar"
java -cp "$TEST_OUTPUT/threat.jar:$PROJECT_DIR/libs/bcprov-ed25519-1.78.1.jar" com.safeer.mobile.browser.ThreatPolicyTestKt
"$KOTLINC" -cp "$PROJECT_DIR/libs/bcprov-ed25519-1.78.1.jar" "$PROJECT_DIR/src/main/kotlin/com/safeer/threatfeed/SignedThreatFeed.kt" "$TEST_DIR/SignedThreatFeedTest.kt" -include-runtime -d "$TEST_OUTPUT/signed-feed.jar"
java -cp "$TEST_OUTPUT/signed-feed.jar:$PROJECT_DIR/libs/bcprov-ed25519-1.78.1.jar" com.safeer.threatfeed.SignedThreatFeedTestKt "$TEST_DIR/signed-feed-conformance"
"$KOTLINC" "$FEED_DIR/BankGuard.kt" "$FEED_DIR/BankGuardData.kt" "$TEST_DIR/BankGuardTest.kt" -include-runtime -d "$TEST_OUTPUT/bank-guard.jar"
java -cp "$TEST_OUTPUT/bank-guard.jar" com.safeer.threatfeed.BankGuardTestKt "$TEST_DIR/bank-guard-cases.json"
"$KOTLINC" "$FEED_DIR/ThreatListAgent.kt" "$TEST_DIR/ThreatListAgentTest.kt" -include-runtime -d "$TEST_OUTPUT/list-agent.jar"
java -cp "$TEST_OUTPUT/list-agent.jar" com.safeer.threatfeed.ThreatListAgentTestKt
"$KOTLINC" "$FEED_DIR/FilterListEngine.kt" "$TEST_DIR/FilterListEngineTest.kt" -include-runtime -d "$TEST_OUTPUT/filter-list.jar"
java -cp "$TEST_OUTPUT/filter-list.jar" com.safeer.threatfeed.FilterListEngineTestKt
"$KOTLINC" "$FEED_DIR/SponsorBlock.kt" "$FEED_DIR/BankGuard.kt" "$FEED_DIR/BankGuardData.kt" "$TEST_DIR/SponsorBlockTest.kt" -include-runtime -d "$TEST_OUTPUT/sponsorblock.jar"
java -cp "$TEST_OUTPUT/sponsorblock.jar" com.safeer.threatfeed.SponsorBlockTestKt
ANDROID_JAR="${ANDROID_JAR:-$PROJECT_DIR/../streamN-TV2/android_tv/.tools/android.jar}"
DNS_CLASSPATH="$ANDROID_JAR"
for lib in "$PROJECT_DIR"/libs/*.jar; do DNS_CLASSPATH="$DNS_CLASSPATH:$lib"; done
"$KOTLINC" -cp "$DNS_CLASSPATH" "$SOURCE_DIR/DoHProxyEngine.kt" "$SOURCE_DIR/LocalDnsProxy.kt" "$SOURCE_DIR/Http2DnsTransport.kt" "$TEST_DIR"/dns-stubs/*.kt "$TEST_DIR/DoHResolverTest.kt" -include-runtime -d "$TEST_OUTPUT/dns.jar"
java -cp "$TEST_OUTPUT/dns.jar:$DNS_CLASSPATH" com.safeer.mobile.browser.DoHResolverTestKt

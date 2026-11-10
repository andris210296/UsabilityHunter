package br.com.uhunter.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UrlSafetyValidatorTest {

	@Test
	void acceptsPublicLiteralIpUrl() {
		// Literal public IPs avoid depending on external DNS in unit tests.
		assertDoesNotThrow(() -> UrlSafetyValidator.requireSafeHttpUrl("https://8.8.8.8/"));
		assertDoesNotThrow(() -> UrlSafetyValidator.requireSafeHttpUrl("https://[2001:4860:4860::8888]/"));
	}

	@Test
	void rejectsBlank() {
		UnsafeUrlException ex = assertThrows(UnsafeUrlException.class,
				() -> UrlSafetyValidator.requireSafeHttpUrl(" "));
		assertTrue(ex.getMessage().toLowerCase().contains("required"));
	}

	@Test
	void rejectsNonHttpScheme() {
		assertThrows(UnsafeUrlException.class,
				() -> UrlSafetyValidator.requireSafeHttpUrl("file:///etc/passwd"));
	}

	@Test
	void rejectsLocalhost() {
		assertThrows(UnsafeUrlException.class,
				() -> UrlSafetyValidator.requireSafeHttpUrl("http://localhost:8080/"));
	}

	@Test
	void rejectsLoopbackIp() {
		assertThrows(UnsafeUrlException.class,
				() -> UrlSafetyValidator.requireSafeHttpUrl("http://127.0.0.1/"));
	}

	@Test
	void rejectsLinkLocalMetadataIp() {
		assertThrows(UnsafeUrlException.class,
				() -> UrlSafetyValidator.requireSafeHttpUrl("http://169.254.169.254/latest/meta-data/"));
	}

	@Test
	void rejectsUserInfo() {
		assertThrows(UnsafeUrlException.class,
				() -> UrlSafetyValidator.requireSafeHttpUrl("https://user:pass@example.com/"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://100.64.0.1/",             // carrier-grade NAT
			"http://0.0.0.1/",                // 0.0.0.0/8
			"http://198.18.0.1/",             // benchmarking
			"http://192.0.0.8/",              // IETF protocol assignments
			"http://240.0.0.1/",              // reserved
			"http://255.255.255.255/",        // broadcast
			"http://2130706433/",             // 127.0.0.1 as a single decimal number
			"http://0x7f.0.0.1/",             // 127.0.0.1 with a hex octet
			"http://[::1]/",                  // IPv6 loopback
			"http://[fd00::1]/",              // unique local
			"http://[fc00::1]/",              // unique local
			"http://[fe80::1]/",              // link local
			"http://[::ffff:127.0.0.1]/",     // IPv4-mapped loopback
			"http://[::ffff:100.64.0.1]/",    // IPv4-mapped CGNAT
			"http://[::127.0.0.1]/",          // IPv4-compatible loopback
			"http://[64:ff9b::7f00:1]/",      // NAT64 -> 127.0.0.1
			"http://[2002:7f00:1::]/"         // 6to4 -> 127.0.0.1
	})
	void rejectsNonPublicTargets(String url) {
		assertThrows(UnsafeUrlException.class, () -> UrlSafetyValidator.requireSafeHttpUrl(url));
	}

	@Test
	void acceptsIpv4MappedPublicAddress() {
		assertDoesNotThrow(() -> UrlSafetyValidator.requireSafeHttpUrl("http://[::ffff:8.8.8.8]/"));
	}

	@Test
	void blocksPrivateAndLoopbackAddresses() throws Exception {
		assertTrue(UrlSafetyValidator.isBlockedAddress(InetAddress.getByName("127.0.0.1")));
		assertTrue(UrlSafetyValidator.isBlockedAddress(InetAddress.getByName("10.0.0.1")));
		assertTrue(UrlSafetyValidator.isBlockedAddress(InetAddress.getByName("192.168.1.1")));
		assertTrue(UrlSafetyValidator.isBlockedAddress(InetAddress.getByName("169.254.169.254")));
	}

	@Test
	void blocksEmbeddedIpv4InRawIpv6Bytes() throws Exception {
		// Built from raw bytes so the JDK keeps them as Inet6Address (no automatic IPv4 unwrapping).
		assertTrue(UrlSafetyValidator.isBlockedAddress(ipv6(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 1)));
		assertTrue(UrlSafetyValidator.isBlockedAddress(ipv6(0, 0x64, 0xFF, 0x9B, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 1)));
		assertFalse(UrlSafetyValidator.isBlockedAddress(ipv6(0, 0x64, 0xFF, 0x9B, 0, 0, 0, 0, 0, 0, 0, 0, 8, 8, 8, 8)));
		assertFalse(UrlSafetyValidator.isBlockedAddress(InetAddress.getByName("8.8.8.8")));
	}

	private static InetAddress ipv6(int... bytes) throws Exception {
		byte[] raw = new byte[16];
		for (int i = 0; i < 16; i++) {
			raw[i] = (byte) bytes[i];
		}
		return InetAddress.getByAddress(raw);
	}
}

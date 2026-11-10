package br.com.uhunter.security;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

/**
 * Guards server-side fetches against SSRF (private/loopback/link-local targets, bad schemes).
 *
 * <p><b>DNS rebinding:</b> the host is resolved here and again by the HTTP client that later
 * connects. The JVM caches successful lookups (see {@code networkaddress.cache.ttl}, pinned in
 * {@code UsabilityHunterApplication}), so the second lookup hits the cache populated by this
 * validation instead of asking an attacker-controlled DNS server again. Every redirect hop is
 * validated right before it is followed, which refreshes that cache entry.
 */
public final class UrlSafetyValidator {

	private static final Set<String> BLOCKED_HOSTS = Set.of(
			"localhost",
			"metadata.google.internal",
			"metadata",
			"kubernetes.default",
			"kubernetes.default.svc");

	private UrlSafetyValidator() {
	}

	/**
	 * @throws UnsafeUrlException when the URL must not be fetched by the server
	 */
	public static URI requireSafeHttpUrl(String rawUrl) {
		if (rawUrl == null || rawUrl.isBlank()) {
			throw new UnsafeUrlException("URL is required");
		}

		final URI uri;
		try {
			uri = new URI(rawUrl.trim()).normalize();
		} catch (URISyntaxException e) {
			throw new UnsafeUrlException("Malformed URL: " + e.getMessage());
		}

		if (uri.getScheme() == null || uri.getHost() == null) {
			throw new UnsafeUrlException("URL must include scheme and host");
		}
		String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
		if (!scheme.equals("http") && !scheme.equals("https")) {
			throw new UnsafeUrlException("Only http and https URLs are allowed");
		}
		if (uri.getUserInfo() != null) {
			throw new UnsafeUrlException("URLs with user info are not allowed");
		}

		String host = uri.getHost().toLowerCase(Locale.ROOT);
		if (BLOCKED_HOSTS.contains(host) || host.endsWith(".localhost") || host.endsWith(".local")) {
			throw new UnsafeUrlException("URL host is not allowed");
		}

		InetAddress[] addresses;
		try {
			addresses = InetAddress.getAllByName(host);
		} catch (UnknownHostException e) {
			throw new UnsafeUrlException("Unable to resolve host: " + host);
		}
		if (addresses.length == 0) {
			throw new UnsafeUrlException("Unable to resolve host: " + host);
		}
		for (InetAddress address : addresses) {
			if (isBlockedAddress(address)) {
				throw new UnsafeUrlException("URL resolves to a private or local network address");
			}
		}
		return uri;
	}

	static boolean isBlockedAddress(InetAddress address) {
		if (address.isAnyLocalAddress()
				|| address.isLoopbackAddress()
				|| address.isLinkLocalAddress()
				|| address.isSiteLocalAddress()
				|| address.isMulticastAddress()) {
			return true;
		}
		byte[] bytes = address.getAddress();
		if (address instanceof Inet4Address) {
			return isBlockedIpv4(bytes, 0);
		}
		if (address instanceof Inet6Address) {
			return isBlockedIpv6(bytes);
		}
		return true;
	}

	/** Ranges {@link InetAddress} does not flag: this-network, CGNAT, IETF/benchmark, reserved. */
	private static boolean isBlockedIpv4(byte[] b, int offset) {
		int a = b[offset] & 0xFF;
		int second = b[offset + 1] & 0xFF;
		int third = b[offset + 2] & 0xFF;
		return a == 0                                           // 0.0.0.0/8 "this network"
				|| (a == 100 && (second & 0xC0) == 64)          // 100.64.0.0/10 carrier-grade NAT
				|| (a == 192 && second == 0 && third == 0)      // 192.0.0.0/24 IETF protocol assignments
				|| (a == 198 && (second & 0xFE) == 18)          // 198.18.0.0/15 benchmarking
				|| a >= 240;                                    // 240.0.0.0/4 reserved + broadcast
	}

	private static boolean isBlockedIpv6(byte[] b) {
		// fc00::/7 unique local addresses
		if ((b[0] & 0xFE) == 0xFC) {
			return true;
		}
		boolean first80Zero = true;
		for (int i = 0; i < 10; i++) {
			if (b[i] != 0) {
				first80Zero = false;
				break;
			}
		}
		int b10 = b[10] & 0xFF;
		int b11 = b[11] & 0xFF;
		// ::ffff:a.b.c.d (IPv4-mapped) and ::a.b.c.d (IPv4-compatible): judge the embedded IPv4.
		if (first80Zero && ((b10 == 0xFF && b11 == 0xFF) || (b10 == 0 && b11 == 0))) {
			return isLoopbackOrBlockedEmbedded(b, 12);
		}
		// 64:ff9b::/96 NAT64: judge the embedded IPv4.
		if ((b[0] & 0xFF) == 0x00 && (b[1] & 0xFF) == 0x64 && (b[2] & 0xFF) == 0xFF && (b[3] & 0xFF) == 0x9B) {
			boolean zeros = true;
			for (int i = 4; i < 12; i++) {
				if (b[i] != 0) {
					zeros = false;
					break;
				}
			}
			if (zeros) {
				return isLoopbackOrBlockedEmbedded(b, 12);
			}
		}
		// 2002::/16 6to4: IPv4 is in bytes 2..5.
		if ((b[0] & 0xFF) == 0x20 && (b[1] & 0xFF) == 0x02) {
			return isLoopbackOrBlockedEmbedded(b, 2);
		}
		return false;
	}

	private static boolean isLoopbackOrBlockedEmbedded(byte[] b, int offset) {
		try {
			byte[] v4 = new byte[] { b[offset], b[offset + 1], b[offset + 2], b[offset + 3] };
			InetAddress embedded = InetAddress.getByAddress(v4);
			return embedded.isAnyLocalAddress()
					|| embedded.isLoopbackAddress()
					|| embedded.isLinkLocalAddress()
					|| embedded.isSiteLocalAddress()
					|| embedded.isMulticastAddress()
					|| isBlockedIpv4(v4, 0);
		} catch (UnknownHostException e) {
			return true;
		}
	}
}

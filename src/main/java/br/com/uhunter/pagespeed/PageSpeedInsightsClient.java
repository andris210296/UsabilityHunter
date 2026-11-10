package br.com.uhunter.pagespeed;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.com.uhunter.config.UhunterProperties;
import br.com.uhunter.utils.ImageUtils;

/**
 * PageSpeed Insights v5 client shared by the whole application.
 *
 * <p>One request makes several checks ask for the same {@code (url, device)} response (the screenshot,
 * the responsive check and the performance check). Responses are therefore cached for
 * {@code uhunter.pagespeed-cache-ttl}, concurrent callers of the same key wait on one in-flight call,
 * and failures are never cached. The cache is bounded by {@code uhunter.pagespeed-cache-max-entries}.
 */
@Component
public class PageSpeedInsightsClient {

	private static final Logger log = LoggerFactory.getLogger(PageSpeedInsightsClient.class);

	private final UhunterProperties properties;
	private final LongSupplier nanoClock;
	private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

	@Autowired
	public PageSpeedInsightsClient(UhunterProperties properties) {
		this(properties, System::nanoTime);
	}

	/** Test seam: lets tests move time forward without sleeping. */
	PageSpeedInsightsClient(UhunterProperties properties, LongSupplier nanoClock) {
		this.properties = properties;
		this.nanoClock = nanoClock;
	}

	public String getOrFetch(String url, boolean mobile) throws IOException {
		String cacheKey = cacheKey(url, mobile);
		CacheEntry mine = new CacheEntry();
		while (true) {
			CacheEntry existing = cache.putIfAbsent(cacheKey, mine);
			if (existing == null) {
				return load(cacheKey, mine, url, mobile);
			}
			if (!existing.isExpired(nanoClock.getAsLong())) {
				return joinCached(existing.future);
			}
			cache.remove(cacheKey, existing);
		}
	}

	private String load(String cacheKey, CacheEntry entry, String url, boolean mobile) throws IOException {
		try {
			String body = fetch(url, mobile);
			entry.complete(body, nanoClock.getAsLong() + ttlNanos());
			if (ttlNanos() <= 0) {
				cache.remove(cacheKey, entry);
			}
			evictIfOversized();
			return body;
		} catch (Exception e) {
			entry.future.completeExceptionally(e);
			cache.remove(cacheKey, entry);
			if (e instanceof IOException io) {
				throw io;
			}
			throw new IOException(e);
		}
	}

	private long ttlNanos() {
		Duration ttl = properties.getPagespeedCacheTtl();
		return ttl == null ? 0 : Math.max(0, ttl.toNanos());
	}

	/** Drops expired entries first, then the entries closest to expiry, until the bound holds again. */
	private void evictIfOversized() {
		int max = Math.max(1, properties.getPagespeedCacheMaxEntries());
		if (cache.size() <= max) {
			return;
		}
		long now = nanoClock.getAsLong();
		cache.entrySet().removeIf(e -> e.getValue().isExpired(now));
		while (cache.size() > max) {
			Optional<Map.Entry<String, CacheEntry>> oldest = cache.entrySet().stream()
					.filter(e -> e.getValue().completed)
					.min(Comparator.comparingLong(e -> e.getValue().expiresAtNanos));
			if (oldest.isEmpty()) {
				return; // only in-flight calls left; never evict those
			}
			cache.remove(oldest.get().getKey(), oldest.get().getValue());
		}
	}

	int cacheSize() {
		return cache.size();
	}

	private static String joinCached(CompletableFuture<String> future) throws IOException {
		try {
			return future.join();
		} catch (CompletionException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			if (cause instanceof IOException io) {
				throw io;
			}
			throw new IOException(cause);
		}
	}

	/** One cached PageSpeed response (or the in-flight call that will produce it). */
	private static final class CacheEntry {
		final CompletableFuture<String> future = new CompletableFuture<>();
		volatile boolean completed;
		volatile long expiresAtNanos;

		void complete(String body, long expiresAt) {
			expiresAtNanos = expiresAt;
			completed = true;
			future.complete(body);
		}

		boolean isExpired(long nowNanos) {
			// In-flight entries never expire; overflow-safe comparison for nanoTime values.
			return completed && nowNanos - expiresAtNanos >= 0;
		}
	}

	public String fetch(String url, boolean mobile) throws IOException {
		String strategy = mobile ? "mobile" : "desktop";
		long started = System.currentTimeMillis();
		HttpTransport httpTransport = new NetHttpTransport();
		HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
		GenericUrl urlApi = new GenericUrl(properties.getPagespeedApiUrl());
		urlApi.put("url", url);
		// Repeated category params: performance (metrics/screenshots) + seo (viewport / taps / fonts).
		urlApi.put("category", new ArrayList<>(List.of("PERFORMANCE", "SEO")));
		urlApi.put("strategy", strategy);
		if (properties.getGoogleApiKey() != null && !properties.getGoogleApiKey().isBlank()) {
			urlApi.put("key", properties.getGoogleApiKey());
		}

		HttpRequest request = requestFactory.buildGetRequest(urlApi)
				.setConnectTimeout(3 * 60000)
				.setReadTimeout(3 * 60000);
		HttpResponse httpResponse = request.execute();
		String body = httpResponse.parseAsString();
		log.info("PageSpeed fetch completed strategy={} url={} in {} ms ({} chars)",
				strategy, url, System.currentTimeMillis() - started, body.length());
		return body;
	}

	/**
	 * Extracts the best available screenshot from a PSI v5 JSON response.
	 * Prefers full-page capture for Vision heuristics; falls back to the smaller final thumbnail.
	 * Skips candidates that cannot be decoded on this JVM (e.g. WebP without arm64 natives).
	 */
	public byte[] extractScreenshot(String pagespeedJson) {
		JsonObject root = JsonParser.parseString(pagespeedJson).getAsJsonObject();
		JsonObject lighthouse = asObject(root.get("lighthouseResult"));
		if (lighthouse == null) {
			throw new IllegalStateException("PageSpeed response has no lighthouseResult");
		}

		String[][] candidates = {
				{ "fullPageScreenshot", readFullPageScreenshotDataUri(lighthouse) },
				{ "full-page-screenshot", readAuditScreenshotDataUri(lighthouse, "full-page-screenshot") },
				{ "final-screenshot", readAuditScreenshotDataUri(lighthouse, "final-screenshot") }
		};

		IllegalStateException lastFailure = null;
		for (String[] candidate : candidates) {
			String source = candidate[0];
			String dataUri = candidate[1];
			if (dataUri == null || dataUri.isBlank()) {
				continue;
			}
			byte[] bytes = decodeDataUri(dataUri);
			if (ImageUtils.isDecodable(bytes)) {
				log.debug("Using PageSpeed screenshot source={} ({} bytes, kind={})",
						source, bytes.length, detectKind(bytes));
				return bytes;
			}
			log.warn("Skipping undecodable PageSpeed screenshot source={} ({} bytes, kind={})",
					source, bytes.length, detectKind(bytes));
			lastFailure = new IllegalStateException(
					"PageSpeed screenshot source=" + source + " could not be decoded on this JVM");
		}

		if (lastFailure != null) {
			throw lastFailure;
		}
		throw new IllegalStateException(
				"PageSpeed response did not include a usable screenshot (final/full-page).");
	}

	private static String detectKind(byte[] bytes) {
		if (ImageUtils.isWebp(bytes)) {
			return "webp";
		}
		if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
			return "jpeg";
		}
		return "unknown";
	}

	private static String readFullPageScreenshotDataUri(JsonObject lighthouse) {
		JsonObject fullPage = asObject(lighthouse.get("fullPageScreenshot"));
		if (fullPage == null) {
			return null;
		}
		JsonObject screenshot = asObject(fullPage.get("screenshot"));
		if (screenshot == null) {
			return null;
		}
		return textOrNull(screenshot.get("data"));
	}

	private static String readAuditScreenshotDataUri(JsonObject lighthouse, String auditId) {
		JsonObject audits = asObject(lighthouse.get("audits"));
		if (audits == null) {
			return null;
		}
		JsonObject audit = asObject(audits.get(auditId));
		if (audit == null) {
			return null;
		}
		JsonObject details = asObject(audit.get("details"));
		if (details == null) {
			return null;
		}
		// Final-screenshot audits carry details.data; the full-page audit nests it as details.screenshot.data.
		String direct = textOrNull(details.get("data"));
		if (direct != null) {
			return direct;
		}
		JsonObject screenshot = asObject(details.get("screenshot"));
		return screenshot == null ? null : textOrNull(screenshot.get("data"));
	}

	private static byte[] decodeDataUri(String dataUri) {
		String payload = dataUri;
		int comma = dataUri.indexOf(',');
		if (dataUri.startsWith("data:") && comma > 0) {
			payload = dataUri.substring(comma + 1);
		}
		return Base64.getDecoder().decode(payload);
	}

	private static String cacheKey(String url, boolean mobile) {
		return (mobile ? "mobile|" : "desktop|") + url;
	}

	private static String textOrNull(JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return null;
		}
		String text = element.getAsString();
		return text == null || text.isBlank() ? null : text;
	}

	private static JsonObject asObject(JsonElement element) {
		if (element == null || element.isJsonNull() || !element.isJsonObject()) {
			return null;
		}
		return element.getAsJsonObject();
	}
}

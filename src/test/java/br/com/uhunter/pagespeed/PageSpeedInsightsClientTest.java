package br.com.uhunter.pagespeed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import br.com.uhunter.config.UhunterProperties;

class PageSpeedInsightsClientTest {

	@Test
	void extractsFinalScreenshotFromAuditWhenOnlyFinalExists() throws Exception {
		byte[] expected = tinyJpeg();
		String dataUri = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(expected);
		String json = """
				{
				  "lighthouseResult": {
				    "audits": {
				      "final-screenshot": {
				        "details": {
				          "data": "%s"
				        }
				      }
				    }
				  }
				}
				""".formatted(dataUri);

		PageSpeedInsightsClient client = new PageSpeedInsightsClient(new UhunterProperties());
		assertArrayEquals(expected, client.extractScreenshot(json));
	}

	@Test
	void prefersFullPageScreenshotOverFinalThumbnail() throws Exception {
		byte[] finalBytes = tinyJpeg(2, 2);
		byte[] fullBytes = tinyJpeg(4, 4);
		String finalUri = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(finalBytes);
		String fullUri = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(fullBytes);
		String json = """
				{
				  "lighthouseResult": {
				    "fullPageScreenshot": {
				      "screenshot": { "data": "%s" }
				    },
				    "audits": {
				      "final-screenshot": {
				        "details": { "data": "%s" }
				      }
				    }
				  }
				}
				""".formatted(fullUri, finalUri);

		PageSpeedInsightsClient client = new PageSpeedInsightsClient(new UhunterProperties());
		assertArrayEquals(fullBytes, client.extractScreenshot(json));
	}

	@Test
	void failsWhenScreenshotMissing() {
		PageSpeedInsightsClient client = new PageSpeedInsightsClient(new UhunterProperties());
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> client.extractScreenshot("{\"lighthouseResult\":{\"audits\":{}}}"));
		assertEquals(true, ex.getMessage().contains("screenshot"));
	}

	@Test
	void readsFullPageScreenshotNestedUnderDetailsScreenshotData() throws Exception {
		byte[] expected = tinyJpeg(4, 4);
		String dataUri = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(expected);
		// Real PSI shape for the full-page audit: no details.data, the image is at details.screenshot.data.
		String json = """
				{ "lighthouseResult": { "audits": {
				    "full-page-screenshot": { "details": { "type": "full-page-screenshot",
				        "screenshot": { "data": "%s", "width": 4, "height": 4 } } } } } }
				""".formatted(dataUri);

		PageSpeedInsightsClient client = new PageSpeedInsightsClient(new UhunterProperties());
		assertArrayEquals(expected, client.extractScreenshot(json));
	}

	// ---- cache -------------------------------------------------------------------------------------

	/** Counts real "HTTP" calls instead of making them, and lets tests move the clock. */
	private static final class CountingClient extends PageSpeedInsightsClient {
		final AtomicInteger fetches = new AtomicInteger();
		volatile IOException failure;
		volatile CountDownLatch gate;

		CountingClient(UhunterProperties properties, AtomicLong clock) {
			super(properties, clock::get);
		}

		@Override
		public String fetch(String url, boolean mobile) throws IOException {
			int n = fetches.incrementAndGet();
			if (gate != null) {
				try {
					gate.await(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			if (failure != null) {
				throw failure;
			}
			return (mobile ? "mobile" : "desktop") + "-" + url + "-" + n;
		}
	}

	private static UhunterProperties cacheProps(Duration ttl, int maxEntries) {
		UhunterProperties properties = new UhunterProperties();
		properties.setPagespeedCacheTtl(ttl);
		properties.setPagespeedCacheMaxEntries(maxEntries);
		return properties;
	}

	@Test
	void sameUrlAndDeviceIsFetchedOnceWithinTheTtl() throws Exception {
		AtomicLong clock = new AtomicLong();
		CountingClient client = new CountingClient(cacheProps(Duration.ofMinutes(5), 16), clock);

		String first = client.getOrFetch("https://a.test", true);
		clock.addAndGet(TimeUnit.MINUTES.toNanos(4));
		String second = client.getOrFetch("https://a.test", true);

		assertEquals(first, second);
		assertEquals(1, client.fetches.get());
	}

	@Test
	void mobileAndDesktopAreSeparateEntries() throws Exception {
		CountingClient client = new CountingClient(cacheProps(Duration.ofMinutes(5), 16), new AtomicLong());

		client.getOrFetch("https://a.test", true);
		client.getOrFetch("https://a.test", false);

		assertEquals(2, client.fetches.get());
	}

	@Test
	void entriesExpireAfterTheTtl() throws Exception {
		AtomicLong clock = new AtomicLong();
		CountingClient client = new CountingClient(cacheProps(Duration.ofMinutes(5), 16), clock);

		client.getOrFetch("https://a.test", false);
		clock.addAndGet(TimeUnit.MINUTES.toNanos(5) + 1);
		client.getOrFetch("https://a.test", false);

		assertEquals(2, client.fetches.get());
	}

	@Test
	void zeroTtlDisablesReuse() throws Exception {
		CountingClient client = new CountingClient(cacheProps(Duration.ZERO, 16), new AtomicLong());

		client.getOrFetch("https://a.test", false);
		client.getOrFetch("https://a.test", false);

		assertEquals(2, client.fetches.get());
		assertEquals(0, client.cacheSize());
	}

	@Test
	void failuresAreNotCached() throws Exception {
		CountingClient client = new CountingClient(cacheProps(Duration.ofMinutes(5), 16), new AtomicLong());
		client.failure = new IOException("quota exceeded");

		assertThrows(IOException.class, () -> client.getOrFetch("https://a.test", false));
		client.failure = null;
		String recovered = client.getOrFetch("https://a.test", false);

		assertEquals(2, client.fetches.get());
		assertEquals(recovered, client.getOrFetch("https://a.test", false));
		assertEquals(2, client.fetches.get());
	}

	@Test
	void concurrentCallersForTheSameKeyShareOneInFlightFetch() throws Exception {
		CountingClient client = new CountingClient(cacheProps(Duration.ofMinutes(5), 16), new AtomicLong());
		client.gate = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(3);
		try {
			Future<String> a = pool.submit(() -> client.getOrFetch("https://a.test", true));
			Future<String> b = pool.submit(() -> client.getOrFetch("https://a.test", true));
			Future<String> c = pool.submit(() -> client.getOrFetch("https://a.test", true));
			Thread.sleep(150); // let all three arrive while the single fetch is still blocked
			client.gate.countDown();

			String result = a.get(5, TimeUnit.SECONDS);
			assertEquals(result, b.get(5, TimeUnit.SECONDS));
			assertEquals(result, c.get(5, TimeUnit.SECONDS));
			assertEquals(1, client.fetches.get());
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void differentKeysDoNotBlockEachOther() throws Exception {
		CountingClient client = new CountingClient(cacheProps(Duration.ofMinutes(5), 16), new AtomicLong());
		client.gate = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<String> mobile = pool.submit(() -> client.getOrFetch("https://a.test", true));
			Future<String> desktop = pool.submit(() -> client.getOrFetch("https://a.test", false));
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (client.fetches.get() < 2 && System.nanoTime() < deadline) {
				Thread.sleep(10);
			}

			assertEquals(2, client.fetches.get(), "mobile and desktop must be fetched in parallel");
			client.gate.countDown();
			mobile.get(5, TimeUnit.SECONDS);
			desktop.get(5, TimeUnit.SECONDS);
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void cacheIsBoundedAndEvictsTheEntryClosestToExpiry() throws Exception {
		AtomicLong clock = new AtomicLong();
		CountingClient client = new CountingClient(cacheProps(Duration.ofMinutes(5), 2), clock);

		String a = client.getOrFetch("https://a.test", false);
		clock.addAndGet(1_000);
		client.getOrFetch("https://b.test", false);
		clock.addAndGet(1_000);
		client.getOrFetch("https://c.test", false);   // over the bound: the oldest ("a") is dropped

		assertEquals(2, client.cacheSize());
		assertEquals(3, client.fetches.get());
		String aAgain = client.getOrFetch("https://a.test", false);
		assertEquals(4, client.fetches.get(), "a was evicted, so it is fetched again");
		assertEquals(false, a.equals(aAgain));
		client.getOrFetch("https://c.test", false);
		assertEquals(4, client.fetches.get(), "c is still cached");
	}

	private static byte[] tinyJpeg() throws IOException {
		return tinyJpeg(1, 1);
	}

	private static byte[] tinyJpeg(int width, int height) throws IOException {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ImageIO.write(image, "jpg", out);
			return out.toByteArray();
		}
	}
}

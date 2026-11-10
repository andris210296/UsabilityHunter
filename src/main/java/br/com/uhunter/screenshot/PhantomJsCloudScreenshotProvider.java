package br.com.uhunter.screenshot;

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import br.com.uhunter.config.UhunterProperties;
import br.com.uhunter.utils.ImageUtils;

/**
 * Legacy screenshot strategy using PhantomJsCloud.
 */
public class PhantomJsCloudScreenshotProvider implements ScreenshotProvider {

	private static final Logger log = LoggerFactory.getLogger(PhantomJsCloudScreenshotProvider.class);
	private static final int MAX_ATTEMPTS = 3;

	private final UhunterProperties properties;
	private final HttpTransport httpTransport = new NetHttpTransport();

	public PhantomJsCloudScreenshotProvider(UhunterProperties properties) {
		this.properties = properties;
	}

	@Override
	public byte[] capture(String url) throws Exception {
		List<String> apiKeys = properties.getPhantomjsCloudApiKeys();
		if (apiKeys == null || apiKeys.isEmpty()) {
			throw new IllegalStateException(
					"No PhantomJsCloud API keys configured. Set uhunter.phantomjs-cloud-api-keys.");
		}

		SocketTimeoutException lastTimeout = null;
		HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			HttpResponse httpResponse = null;
			try {
				String apiUrl = String.format(properties.getPhantomjsCloudApiUrlTemplate(),
						apiKeys.get(ThreadLocalRandom.current().nextInt(apiKeys.size())));

				GenericUrl urlApi = new GenericUrl(apiUrl);
				JSONObject json = new JSONObject();
				json.put("url", url);
				json.put("renderType", "jpg");
				json.put("outputAsJson", false);

				HttpRequest request = requestFactory
						.buildPostRequest(urlApi, ByteArrayContent.fromString("application/json", json.toString()))
						.setConnectTimeout(3 * 60000)
						.setReadTimeout(3 * 60000);

				httpResponse = request.execute();
				try (InputStream inputStream = httpResponse.getContent()) {
					return ImageUtils.inputStreamToByteArray(inputStream);
				}
			} catch (SocketTimeoutException e) {
				lastTimeout = e;
				log.warn("PhantomJsCloud timeout for url={} attempt={}/{}: {}",
						url, attempt, MAX_ATTEMPTS, e.getMessage());
			} finally {
				if (httpResponse != null) {
					httpResponse.disconnect();
				}
			}
		}
		throw lastTimeout != null
				? lastTimeout
				: new IllegalStateException("PhantomJsCloud capture failed for url=" + url);
	}
}

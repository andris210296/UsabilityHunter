package br.com.uhunter.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

import br.com.uhunter.screenshot.ScreenshotProviderType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "uhunter")
public class UhunterProperties {

	private String googleApiKey = "";
	/**
	 * Absolute path to a service-account JSON for Cloud Vision.
	 */
	private String googleApplicationCredentials = "";
	/**
	 * Screenshot strategy: {@code pagespeed} (default) or {@code phantomjs}.
	 */
	private ScreenshotProviderType screenshotProvider = ScreenshotProviderType.PAGESPEED;
	private List<String> phantomjsCloudApiKeys = new ArrayList<>();
	private String pagespeedApiUrl;
	private String phantomjsCloudApiUrlTemplate;
	/**
	 * Upper bound for the four parallel heuristic checks of one request (PageSpeed alone can take minutes).
	 */
	private Duration validationTimeout = Duration.ofMinutes(4);
	/**
	 * How long a PageSpeed response is reused (per url + device). Must cover one request, because the
	 * screenshot, responsive and performance checks share the same response; it also saves quota on
	 * repeated requests for the same page. {@code 0} drops entries as soon as they complete.
	 */
	private Duration pagespeedCacheTtl = Duration.ofMinutes(5);
	/**
	 * Upper bound on cached PageSpeed responses (each can be several MB because of embedded screenshots).
	 */
	private int pagespeedCacheMaxEntries = 16;

	public void setPhantomjsCloudApiKeys(List<String> phantomjsCloudApiKeys) {
		if (phantomjsCloudApiKeys == null) {
			this.phantomjsCloudApiKeys = new ArrayList<>();
			return;
		}
		this.phantomjsCloudApiKeys = phantomjsCloudApiKeys.stream()
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}

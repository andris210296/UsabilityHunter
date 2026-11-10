package br.com.uhunter.screenshot;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import br.com.uhunter.config.ScreenshotConfig;
import br.com.uhunter.config.UhunterProperties;
import br.com.uhunter.pagespeed.PageSpeedInsightsClient;

class ScreenshotConfigTest {

	private final ScreenshotConfig config = new ScreenshotConfig();

	@Test
	void defaultsToPageSpeed() {
		UhunterProperties properties = new UhunterProperties();

		ScreenshotProvider provider = config.screenshotProvider(properties, new PageSpeedInsightsClient(properties));

		assertInstanceOf(PageSpeedScreenshotProvider.class, provider);
	}

	@Test
	void canSwitchToPhantomJs() {
		UhunterProperties properties = new UhunterProperties();
		properties.setScreenshotProvider(ScreenshotProviderType.PHANTOMJS);

		ScreenshotProvider provider = config.screenshotProvider(properties, new PageSpeedInsightsClient(properties));

		assertInstanceOf(PhantomJsCloudScreenshotProvider.class, provider);
	}

	@Test
	void propertyBindsCaseInsensitivelyFromTheDocumentedValues() {
		UhunterProperties bound = bind("uhunter.screenshot-provider", "phantomjs");
		org.junit.jupiter.api.Assertions.assertEquals(ScreenshotProviderType.PHANTOMJS, bound.getScreenshotProvider());
	}

	@Test
	void unknownProviderFailsAtBindTimeInsteadOfSilentlyFallingBack() {
		org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
				() -> bind("uhunter.screenshot-provider", "selenium"));
	}

	private static UhunterProperties bind(String key, String value) {
		return new Binder(new MapConfigurationPropertySource(java.util.Map.of(key, value)))
				.bind("uhunter", UhunterProperties.class)
				.get();
	}
}

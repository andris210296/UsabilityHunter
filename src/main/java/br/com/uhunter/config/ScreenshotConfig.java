package br.com.uhunter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.uhunter.pagespeed.PageSpeedInsightsClient;
import br.com.uhunter.screenshot.PageSpeedScreenshotProvider;
import br.com.uhunter.screenshot.PhantomJsCloudScreenshotProvider;
import br.com.uhunter.screenshot.ScreenshotProvider;

/**
 * Picks the single {@link ScreenshotProvider} bean from {@code uhunter.screenshot-provider}.
 */
@Configuration
public class ScreenshotConfig {

	@Bean
	public ScreenshotProvider screenshotProvider(UhunterProperties properties, PageSpeedInsightsClient pageSpeedClient) {
		return switch (properties.getScreenshotProvider()) {
			case PAGESPEED -> new PageSpeedScreenshotProvider(pageSpeedClient);
			case PHANTOMJS -> new PhantomJsCloudScreenshotProvider(properties);
		};
	}
}

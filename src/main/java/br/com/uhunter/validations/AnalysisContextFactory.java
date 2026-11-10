package br.com.uhunter.validations;

import org.springframework.stereotype.Component;

import br.com.uhunter.page.PageDocument;
import br.com.uhunter.pagespeed.PageSpeedInsightsClient;
import br.com.uhunter.screenshot.Screenshot;
import br.com.uhunter.screenshot.ScreenshotProvider;

/**
 * Does the shared, slow, I/O-heavy setup (HTML fetch, screenshot) exactly once per request so the checks
 * themselves can run in parallel on ready-made inputs.
 */
@Component
public class AnalysisContextFactory {

	private final PageSpeedInsightsClient pageSpeedClient;
	private final ScreenshotProvider screenshotProvider;

	public AnalysisContextFactory(PageSpeedInsightsClient pageSpeedClient, ScreenshotProvider screenshotProvider) {
		this.pageSpeedClient = pageSpeedClient;
		this.screenshotProvider = screenshotProvider;
	}

	/**
	 * @param safeUrl a URL already accepted by {@code UrlSafetyValidator}
	 */
	public AnalysisContext create(String safeUrl) throws Exception {
		PageDocument page = PageDocument.fetch(safeUrl);
		Screenshot screenshot = Screenshot.of(screenshotProvider.capture(safeUrl));
		return new AnalysisContext(safeUrl, page, pageSpeedClient, screenshot);
	}
}

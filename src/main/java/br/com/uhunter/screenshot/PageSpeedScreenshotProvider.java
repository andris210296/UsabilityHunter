package br.com.uhunter.screenshot;

import br.com.uhunter.pagespeed.PageSpeedInsightsClient;

/**
 * Captures the desktop screenshot from PageSpeed Insights / Lighthouse output. The PageSpeed response is
 * shared (via the client's cache) with the performance and responsive checks.
 */
public class PageSpeedScreenshotProvider implements ScreenshotProvider {

	private final PageSpeedInsightsClient pageSpeedInsightsClient;

	public PageSpeedScreenshotProvider(PageSpeedInsightsClient pageSpeedInsightsClient) {
		this.pageSpeedInsightsClient = pageSpeedInsightsClient;
	}

	@Override
	public byte[] capture(String url) throws Exception {
		String json = pageSpeedInsightsClient.getOrFetch(url, false);
		return pageSpeedInsightsClient.extractScreenshot(json);
	}
}

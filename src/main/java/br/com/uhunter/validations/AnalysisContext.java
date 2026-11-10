package br.com.uhunter.validations;

import br.com.uhunter.page.PageDocument;
import br.com.uhunter.pagespeed.PageSpeedInsightsClient;
import br.com.uhunter.screenshot.Screenshot;

/**
 * Everything one analysis run shares, built once per request by {@link AnalysisContextFactory}.
 * Every {@link UsabilityCheck} takes just this, so adding a check never changes the orchestrator's wiring.
 *
 * @param url         the validated URL under test
 * @param page        the page HTML, fetched once
 * @param pageSpeed   PageSpeed client (application-wide; responses are cached per url and device)
 * @param screenshot  the page screenshot, captured once
 */
public record AnalysisContext(
		String url,
		PageDocument page,
		PageSpeedInsightsClient pageSpeed,
		Screenshot screenshot) {
}

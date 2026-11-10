package br.com.uhunter.validations.responsive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import br.com.uhunter.api.dto.ResponsiveValidationResultDto;
import br.com.uhunter.config.UhunterProperties;
import br.com.uhunter.page.PageDocument;
import br.com.uhunter.pagespeed.PageSpeedInsightsClient;
import br.com.uhunter.validations.AnalysisContext;

class ResponsiveCheckTest {

	private static final String URL = "https://example.com";

	/** PageSpeed is unreachable: the check must fall back to HTML heuristics only. */
	private static final PageSpeedInsightsClient UNAVAILABLE_PAGESPEED =
			new PageSpeedInsightsClient(new UhunterProperties()) {
				@Override
				public String getOrFetch(String url, boolean mobile) throws IOException {
					throw new IOException("pagespeed down");
				}
			};

	@Test
	void usesHtmlHeuristicsWhenPageSpeedIsUnavailable() {
		PageDocument page = PageDocument.fromHtml(URL, """
				<html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
				<body><h1>Hi</h1></body></html>""");

		ResponsiveValidationResultDto result =
				new ResponsiveCheck().run(new AnalysisContext(URL, page, UNAVAILABLE_PAGESPEED, null));

		assertEquals(true, result.isMobileFriendly());
	}

	@Test
	void flagsMissingViewport() {
		PageDocument page = PageDocument.fromHtml(URL, "<html><head></head><body>Hi</body></html>");

		ResponsiveValidationResultDto result =
				new ResponsiveCheck().run(new AnalysisContext(URL, page, UNAVAILABLE_PAGESPEED, null));

		assertEquals(false, result.isMobileFriendly());
		assertTrue(result.listOfIssues().containsKey("CONFIGURE_VIEWPORT"));
	}
}

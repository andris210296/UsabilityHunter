package br.com.uhunter.validations.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.uhunter.api.dto.PerformanceDevicesResultDto;
import br.com.uhunter.api.dto.PerformanceMetricDto;
import br.com.uhunter.api.dto.PerformanceValidationResultDto;
import br.com.uhunter.config.UhunterProperties;
import br.com.uhunter.pagespeed.PageSpeedInsightsClient;
import br.com.uhunter.validations.AnalysisContext;

class PerformanceCheckTest {

	private static final String URL = "https://example.com";

	private static final String LAB_ONLY_RESPONSE = """
			{ "lighthouseResult": {
			    "categories": { "performance": { "score": 0.9 } },
			    "audits": {
			      "first-contentful-paint": { "numericValue": 1000 },
			      "largest-contentful-paint": { "numericValue": 3000 }
			    } } }""";

	private static PageSpeedInsightsClient clientReturning(String json) {
		return new PageSpeedInsightsClient(new UhunterProperties()) {
			@Override
			public String getOrFetch(String url, boolean mobile) {
				return json;
			}
		};
	}

	@Test
	void buildsMobileAndDesktopResultsFromPageSpeed() {
		AnalysisContext context = new AnalysisContext(URL, null, clientReturning(LAB_ONLY_RESPONSE), null);

		PerformanceValidationResultDto result = new PerformanceCheck().run(context);

		PerformanceDevicesResultDto devices = assertInstanceOf(PerformanceDevicesResultDto.class, result.performanceTest());
		assertEquals(10, devices.mobileTest().optimizationPoints());   // (1 - 0.9) * 100
		assertEquals("FAST", devices.desktopTest().loadingOverall());
		List<PerformanceMetricDto> metrics = devices.mobileTest().loadingPerformance().get("metrics");
		assertEquals(2, metrics.size());
		assertEquals(1000, metrics.get(0).pageSpeed());
		assertEquals("FAST", metrics.get(0).pageContentLoadDistributionAverage());
		assertEquals("AVERAGE", metrics.get(1).pageContentLoadDistributionAverage());   // LCP 3000 ms
	}

	@Test
	void pageSpeedFailureBecomesAFallbackResultNotAnException() {
		PageSpeedInsightsClient down = new PageSpeedInsightsClient(new UhunterProperties()) {
			@Override
			public String getOrFetch(String url, boolean mobile) throws IOException {
				throw new IOException("quota exceeded");
			}
		};

		PerformanceValidationResultDto result = new PerformanceCheck().run(new AnalysisContext(URL, null, down, null));

		assertTrue(String.valueOf(result.performanceTest()).contains("quota exceeded"));
	}
}

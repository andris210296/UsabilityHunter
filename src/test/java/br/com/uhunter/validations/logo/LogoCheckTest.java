package br.com.uhunter.validations.logo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.uhunter.FakeVision;
import br.com.uhunter.TestImages;
import br.com.uhunter.api.dto.LogoValidationResultDto;
import br.com.uhunter.page.PageDocument;
import br.com.uhunter.screenshot.Screenshot;
import br.com.uhunter.validations.AnalysisContext;

class LogoCheckTest {

	private static final String URL = "https://www.wikipedia.org";

	private static AnalysisContext context() throws Exception {
		PageDocument page = PageDocument.fromHtml(URL, "<html><head><title>Wikipedia</title></head></html>");
		return new AnalysisContext(URL, page, null, Screenshot.of(TestImages.jpeg(1400, 1000)));
	}

	@Test
	void reportsLogoFoundInTheTopLeftRegion() throws Exception {
		FakeVision vision = new FakeVision() {
			@Override
			protected List<String> logos(int call) {
				return List.of("Wikipedia");
			}
		};

		LogoValidationResultDto result = new LogoCheck(vision).run(context());

		assertEquals(true, result.result());
		assertEquals("Wikipedia", result.logoName());
		assertEquals(1, vision.logoCalls.get(), "a hit on the whole region must not trigger the quadrant search");
	}

	@Test
	void searchesTheQuadrantsWhenTheWholeRegionMisses() throws Exception {
		FakeVision vision = new FakeVision() {
			@Override
			protected List<String> logos(int call) {
				// Call 1 = whole top-left region (miss); call 3 = second quadrant (hit).
				return call == 3 ? List.of("Wikipedia") : List.of();
			}
		};

		LogoValidationResultDto result = new LogoCheck(vision).run(context());

		assertEquals(true, result.result());
		assertEquals("Wikipedia", result.logoName());
		assertEquals(3, vision.logoCalls.get(), "stops at the first quadrant that hits");
	}

	@Test
	void reportsNoLogoWhenNothingMatches() throws Exception {
		FakeVision vision = new FakeVision();

		LogoValidationResultDto result = new LogoCheck(vision).run(context());

		assertFalse((Boolean) result.result());
		assertEquals(5, vision.logoCalls.get(), "whole region + four quadrants");
	}

	@Test
	void visionFailureBecomesAFallbackResultNotAnException() throws Exception {
		FakeVision vision = new FakeVision() {
			@Override
			protected List<String> logos(int call) throws Exception {
				throw new IllegalStateException("vision is down");
			}
		};

		LogoValidationResultDto result = new LogoCheck(vision).run(context());

		assertEquals(false, result.result());
		assertEquals("vision is down", result.logoName());
		assertEquals(0.0, result.confidence());
		assertTrue(result.evidence().contains("failed"));
	}
}

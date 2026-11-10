package br.com.uhunter.validations.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.uhunter.FakeVision;
import br.com.uhunter.TestImages;
import br.com.uhunter.api.dto.NavigationValidationResultDto;
import br.com.uhunter.page.PageDocument;
import br.com.uhunter.screenshot.Screenshot;
import br.com.uhunter.validations.AnalysisContext;
import br.com.uhunter.vision.VisionTextBlock;

class NavigationCheckTest {

	private static final String URL = "https://example.com";

	private static AnalysisContext context() throws Exception {
		PageDocument page = PageDocument.fromHtml(URL, "<html><body><nav><a href='/a'>Arts</a></nav></body></html>");
		return new AnalysisContext(URL, page, null, Screenshot.of(TestImages.jpeg(1400, 1000)));
	}

	@Test
	void reportsNoLeftNavigationWhenVisionSeesNoText() throws Exception {
		NavigationValidationResultDto result = new NavigationCheck(new FakeVision()).run(context());

		assertEquals(false, result.navigationOnLeftCorner());
		assertEquals("not applicable", result.hicksLaw());
	}

	@Test
	void visionFailureBecomesAFallbackResultNotAnException() throws Exception {
		FakeVision broken = new FakeVision() {
			@Override
			public List<VisionTextBlock> detectTextBlocks(byte[] image) {
				throw new IllegalStateException("vision is down");
			}
		};

		NavigationValidationResultDto result = new NavigationCheck(broken).run(context());

		assertEquals("vision is down", result.navigationOnLeftCorner());
		assertEquals(0.0, result.confidence());
		assertTrue(result.evidence().contains("failed"));
	}
}

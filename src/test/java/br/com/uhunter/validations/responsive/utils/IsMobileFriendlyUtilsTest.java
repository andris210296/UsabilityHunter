package br.com.uhunter.validations.responsive.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.uhunter.api.dto.ResponsiveValidationResultDto;
import br.com.uhunter.validations.responsive.utils.IsMobileFriendlyUtils.IssuesEnum;
import br.com.uhunter.page.PageDocument;

class IsMobileFriendlyUtilsTest {

	@Test
	void resultShapeMatchesApiContract() {
		ResponsiveValidationResultDto ok = ResponsiveValidationResultDto.builder()
				.isMobileFriendly(true)
				.listOfIssues(null)
				.confidence(0.85)
				.evidence("viewport ok")
				.build();
		assertTrue((Boolean) ok.isMobileFriendly());
		assertNull(ok.listOfIssues());
		assertEquals(0.85, ok.confidence());
	}

	@Test
	void testHtmlWithViewportIsMobileFriendly() {
		ResponsiveValidationResultDto result = IsMobileFriendlyUtils.analyzeHtml("""
				<html><head>
				<meta name="viewport" content="width=device-width, initial-scale=1">
				</head><body><h1>Ok</h1></body></html>
				""");

		assertTrue((Boolean) result.isMobileFriendly());
		assertNull(result.listOfIssues());
	}

	@Test
	void testHtmlMissingViewport() {
		ResponsiveValidationResultDto result = IsMobileFriendlyUtils.analyzeHtml(
				"<html><head></head><body>Hi</body></html>");

		assertFalse((Boolean) result.isMobileFriendly());
		assertEquals(
				List.of(IssuesEnum.CONFIGURE_VIEWPORT.name()),
				new ArrayList<>(result.listOfIssues().keySet()));
	}

	@Test
	void testFixedWidthViewport() {
		ResponsiveValidationResultDto result = IsMobileFriendlyUtils.analyzeHtml("""
				<html><head>
				<meta name="viewport" content="width=980">
				</head><body></body></html>
				""");

		assertFalse((Boolean) result.isMobileFriendly());
		assertEquals(
				List.of(IssuesEnum.FIXED_WIDTH_VIEWPORT.name()),
				new ArrayList<>(result.listOfIssues().keySet()));
	}

	@Test
	void testTinyTapTargets() {
		ResponsiveValidationResultDto result = IsMobileFriendlyUtils.analyzeHtml("""
				<html><head>
				<meta name="viewport" content="width=device-width, initial-scale=1">
				</head><body>
				<a href="/1" width="20" height="20">1</a>
				<a href="/2" width="20" height="20">2</a>
				<a href="/3" width="20" height="20">3</a>
				</body></html>
				""");
		assertFalse((Boolean) result.isMobileFriendly());
		assertTrue(result.listOfIssues().containsKey(IssuesEnum.TAP_TARGETS_TOO_CLOSE.name()));
	}

	@Test
	void testIssueListExplanation() {
		Map<String, String> explained = IsMobileFriendlyUtils.explainIssues(List.of(
				"CONFIGURE_VIEWPORT",
				"FIXED_WIDTH_VIEWPORT"));

		assertEquals(2, explained.size());
		assertEquals(IssuesEnum.CONFIGURE_VIEWPORT.getValue(),
				explained.get(IssuesEnum.CONFIGURE_VIEWPORT.toString()));
	}

	@Test
	void analyzesAnAlreadyFetchedPageDocument() {
		PageDocument page = PageDocument.fromHtml("https://example.com", """
				<html><head>
				<meta name="viewport" content="width=device-width, initial-scale=1">
				</head><body>ok</body></html>
				""");
		ResponsiveValidationResultDto result = IsMobileFriendlyUtils.analyze(page);
		assertTrue((Boolean) result.isMobileFriendly());
		assertNull(result.listOfIssues());
	}

	@Test
	void detectsTinyInlineFont() {
		PageDocument page = PageDocument.fromHtml("https://example.com", """
				<html><head>
				<meta name="viewport" content="width=device-width, initial-scale=1">
				</head><body><p style="font-size: 9px">tiny</p></body></html>
				""");
		ResponsiveValidationResultDto result = IsMobileFriendlyUtils.analyze(page);
		assertFalse((Boolean) result.isMobileFriendly());
		assertTrue(result.listOfIssues().containsKey("USE_LEGIBLE_FONT_SIZES"));
	}

	@Test
	void pagespeedViewportPassClearsHtmlConfigureViewportFalseNegative() {
		Set<String> issues = new LinkedHashSet<>();
		issues.add(IssuesEnum.CONFIGURE_VIEWPORT.name());
		List<String> notes = new ArrayList<>();
		String json = """
				{
				  "lighthouseResult": {
				    "audits": {
				      "viewport": { "score": 1 },
				      "content-width": { "score": 1 },
				      "font-size": { "score": 1 },
				      "tap-targets": { "score": 1 }
				    }
				  }
				}
				""";
		assertTrue(IsMobileFriendlyUtils.applyLighthouseMobileAudits(json, issues, notes));
		assertTrue(issues.isEmpty());
		assertTrue(notes.stream().anyMatch(n -> n.contains("viewport ok")));
	}

	@Test
	void pagespeedViewportFailKeepsConfigureViewportIssue() {
		Set<String> issues = new LinkedHashSet<>();
		List<String> notes = new ArrayList<>();
		String json = """
				{
				  "lighthouseResult": {
				    "audits": {
				      "viewport": { "score": 0 }
				    }
				  }
				}
				""";
		assertTrue(IsMobileFriendlyUtils.applyLighthouseMobileAudits(json, issues, notes));
		assertTrue(issues.contains(IssuesEnum.CONFIGURE_VIEWPORT.name()));
	}

	@Test
	void spaSoftPassClearsHtmlViewportWhenAuditMissing() {
		Set<String> issues = new LinkedHashSet<>();
		issues.add(IssuesEnum.CONFIGURE_VIEWPORT.name());
		List<String> notes = new ArrayList<>();
		String json = """
				{
				  "lighthouseResult": {
				    "audits": {
				      "first-contentful-paint": { "score": 0.5 }
				    }
				  }
				}
				""";
		IsMobileFriendlyUtils.softenMissingViewportMeta(json, issues, notes);
		assertTrue(issues.isEmpty());
		assertTrue(notes.get(0).contains("spa soft-pass"));
	}

	@Test
	void spaSoftPassDoesNotClearWhenViewportAuditFailed() {
		Set<String> issues = new LinkedHashSet<>();
		issues.add(IssuesEnum.CONFIGURE_VIEWPORT.name());
		List<String> notes = new ArrayList<>();
		String json = """
				{
				  "lighthouseResult": {
				    "audits": {
				      "viewport": { "score": 0 }
				    }
				  }
				}
				""";
		IsMobileFriendlyUtils.softenMissingViewportMeta(json, issues, notes);
		assertTrue(issues.contains(IssuesEnum.CONFIGURE_VIEWPORT.name()));
	}
}

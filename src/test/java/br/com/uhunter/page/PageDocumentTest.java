package br.com.uhunter.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PageDocumentTest {

	@Test
	void extractsBrandCandidatesAndNavLandmarks() {
		String html = """
				<html><head>
				<title>Wikipedia</title>
				<meta property="og:site_name" content="Wikimedia">
				</head><body>
				<header><img alt="Wikipedia logo" src="/logo.png"></header>
				<nav class="navigation"><a href="/a">Arts</a><a href="/b">Biography</a></nav>
				</body></html>
				""";
		PageDocument page = PageDocument.fromHtml("https://example.com", html);
		assertTrue(page.brandCandidates().contains("Wikipedia"));
		assertTrue(page.brandCandidates().contains("Wikimedia"));
		assertTrue(page.brandCandidates().contains("Wikipedia logo"));
		assertEquals(2, page.navigationLandmarkTexts().size());
		assertEquals(2, page.linksByText().size());
	}

	@Test
	void ignoresBroadNavClassNoise() {
		String html = """
				<html><body>
				<div class="nav"><a href="/x">Everywhere</a></div>
				<nav><a href="/a">Real nav</a></nav>
				</body></html>
				""";
		PageDocument page = PageDocument.fromHtml("https://www5.usp.br/", html);
		assertEquals(1, page.navigationLandmarkTexts().size());
		assertEquals("Real nav", page.navigationLandmarkTexts().get(0));
		assertTrue(page.brandCandidates().contains("usp"));
	}

	@Test
	void extractsAcronymFromTitle() {
		PageDocument page = PageDocument.fromHtml(
				"https://www5.usp.br/",
				"<html><head><title>USP – Universidade de São Paulo</title></head><body></body></html>");
		assertTrue(page.brandCandidates().contains("USP"));
		assertTrue(page.brandCandidates().contains("usp"));
	}

	@Test
	void normalizeForCompareStripsNoise() {
		assertEquals("wikipedia", PageDocument.normalizeForCompare(" Wiki-Pédia "));
	}
}

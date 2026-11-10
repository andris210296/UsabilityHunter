package br.com.uhunter.page;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.uhunter.security.UrlSafetyValidator;

/**
 * Single HTML fetch shared by logo, navigation, and responsive checks.
 */
public final class PageDocument {

	private static final int MAX_REDIRECTS = 5;

	private final String url;
	private final Document document;

	private PageDocument(String url, Document document) {
		this.url = url;
		this.document = document;
	}

	public static PageDocument fetch(String url) throws IOException {
		String current = UrlSafetyValidator.requireSafeHttpUrl(url).toString();
		for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
			Connection.Response response = Jsoup.connect(current)
					.userAgent("Mozilla/5.0 (compatible; UsabilityHunter/1.0)")
					.timeout(15000)
					.followRedirects(false)
					.ignoreHttpErrors(true)
					.execute();
			int status = response.statusCode();
			if (status >= 300 && status < 400) {
				String location = response.header("Location");
				if (location == null || location.isBlank()) {
					throw new IOException("Redirect without Location header from " + current);
				}
				URI next = URI.create(current).resolve(location);
				current = UrlSafetyValidator.requireSafeHttpUrl(next.toString()).toString();
				continue;
			}
			if (status >= 400) {
				throw new IOException("HTTP error fetching URL. Status=" + status + ", URL=[" + current + "]");
			}
			return new PageDocument(current, response.parse());
		}
		throw new IOException("Too many redirects fetching URL=[" + url + "]");
	}

	public static PageDocument fromHtml(String url, String html) {
		return new PageDocument(url, Jsoup.parse(html, url));
	}

	public String url() {
		return url;
	}

	public Document document() {
		return document;
	}

	public String title() {
		return document.title();
	}

	public String siteName() {
		Element og = document.selectFirst("meta[property=og:site_name]");
		return og == null ? "" : og.attr("content").trim();
	}

	/**
	 * Brand-like strings used to validate OCR / logo detection against the page identity.
	 */
	public List<String> brandCandidates() {
		LinkedHashSet<String> candidates = new LinkedHashSet<>();
		addIfPresent(candidates, hostBrandHint());
		addIfPresent(candidates, siteName());
		addIfPresent(candidates, title());
		for (String acronym : extractAcronyms(title())) {
			addIfPresent(candidates, acronym);
		}
		for (String acronym : extractAcronyms(siteName())) {
			addIfPresent(candidates, acronym);
		}

		Element header = document.selectFirst("header, [role=banner], #header, .header");
		Elements alts = header != null
				? header.select("img[alt]")
				: document.select("img[alt]");
		int limit = 0;
		for (Element img : alts) {
			addIfPresent(candidates, img.attr("alt"));
			if (++limit >= 5) {
				break;
			}
		}
		return new ArrayList<>(candidates);
	}

	/** Leading/all-caps tokens like {@code USP} from titles such as {@code USP – Universidade...}. */
	static List<String> extractAcronyms(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		List<String> acronyms = new ArrayList<>();
		for (String token : value.split("[^A-Za-z0-9]+")) {
			if (token.length() >= 2 && token.length() <= 6 && token.equals(token.toUpperCase(Locale.ROOT))) {
				acronyms.add(token);
			}
		}
		return acronyms;
	}

	/** Visible link text (or aria-label/title for icon links) → absolute href. */
	public Map<String, String> linksByText() {
		LinkedHashMap<String, String> links = new LinkedHashMap<>();
		for (Element link : document.select("a[href]")) {
			String text = normalizeWhitespace(link.text());
			if (text.isEmpty()) {
				text = normalizeWhitespace(link.attr("aria-label"));
			}
			if (text.isEmpty()) {
				text = normalizeWhitespace(link.attr("title"));
			}
			if (text.isEmpty() || text.length() > 80) {
				continue;
			}
			links.putIfAbsent(text, link.attr("abs:href"));
		}
		return links;
	}

	/**
	 * Texts found in semantic / common navigation landmarks.
	 * Keep the selector tight — broad classes like {@code .nav} match half the modern DOM.
	 */
	public List<String> navigationLandmarkTexts() {
		LinkedHashSet<String> texts = new LinkedHashSet<>();
		Elements navs = document.select(
				"nav, [role=navigation], aside[role=complementary] nav, #sidebar, .sidebar, #navigation, #menu-main");
		int navBudget = 0;
		for (Element nav : navs) {
			if (++navBudget > 4) {
				break;
			}
			int linkBudget = 0;
			for (Element link : nav.select("> ul > li > a[href], > ol > li > a[href], > a[href], a[href]")) {
				String text = normalizeWhitespace(link.text());
				if (text.length() < 2 || text.length() > 60) {
					continue;
				}
				addIfPresent(texts, text);
				if (++linkBudget >= 40) {
					break;
				}
			}
		}
		return new ArrayList<>(texts);
	}

	/**
	 * Host label used as an extra brand hint (e.g. {@code usp} from {@code www5.usp.br}).
	 */
	public String hostBrandHint() {
		try {
			String host = URI.create(url).getHost();
			if (host == null || host.isBlank()) {
				return "";
			}
			String[] parts = host.toLowerCase(Locale.ROOT).split("\\.");
			for (String part : parts) {
				if (part.equals("www") || part.equals("www2") || part.equals("www5") || part.length() < 2) {
					continue;
				}
				if (part.matches("^[a-z0-9-]{2,}$") && !part.matches("^\\d+$")) {
					return part.replace("-", " ");
				}
			}
		} catch (Exception ignored) {
			// ignore malformed URLs in tests
		}
		return "";
	}

	private static void addIfPresent(Set<String> target, String value) {
		String cleaned = normalizeWhitespace(value);
		if (!cleaned.isEmpty()) {
			target.add(cleaned);
		}
	}

	private static String normalizeWhitespace(String value) {
		if (value == null) {
			return "";
		}
		return value.replaceAll("\\s+", " ").trim();
	}

	public static String normalizeForCompare(String value) {
		if (value == null) {
			return "";
		}
		String ascii = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
				.replaceAll("[^\\p{ASCII}]", "");
		return ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
	}
}

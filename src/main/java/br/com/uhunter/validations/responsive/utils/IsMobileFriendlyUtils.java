package br.com.uhunter.validations.responsive.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.com.uhunter.api.dto.ResponsiveValidationResultDto;
import br.com.uhunter.pagespeed.PageSpeedInsightsClient;
import br.com.uhunter.page.PageDocument;
import lombok.Getter;

/**
 * Mobile-friendly check: HTML heuristics refined by PageSpeed/Lighthouse mobile SEO audits
 * when available (avoids SPA false negatives like missing viewport meta on YouTube).
 */
public final class IsMobileFriendlyUtils {

	private static final Logger log = LoggerFactory.getLogger(IsMobileFriendlyUtils.class);

	private static final Pattern FIXED_WIDTH = Pattern.compile("(?i)width\\s*=\\s*\\d+");
	private static final Pattern FONT_SIZE_PX = Pattern.compile("(?i)font-size\\s*:\\s*(\\d+(?:\\.\\d+)?)px");
	private static final Pattern WIDTH_HEIGHT_ATTR = Pattern.compile("^\\d+$");

	private static final int MIN_LEGIBLE_FONT_PX = 12;
	private static final int MIN_TAP_TARGET_PX = 48;
	private static final int MAX_CONTENT_WIDTH_HINT = 980;
	private static final double LIGHTHOUSE_PASS_SCORE = 0.9;

	private IsMobileFriendlyUtils() {
	}

	public static ResponsiveValidationResultDto analyze(PageDocument page) {
		return analyze(page, null);
	}

	public static ResponsiveValidationResultDto analyze(PageDocument page, PageSpeedInsightsClient pageSpeedClient) {
		Set<String> issues = collectHtmlIssues(page.document());
		List<String> notes = new ArrayList<>();
		boolean usedPageSpeed = false;

		if (pageSpeedClient != null && page.url() != null && !page.url().isBlank()) {
			try {
				String json = pageSpeedClient.getOrFetch(page.url(), true);
				if (hasLighthouseResult(json)) {
					usedPageSpeed = true;
					applyLighthouseMobileAudits(json, issues, notes);
					softenMissingViewportMeta(json, issues, notes);
				}
			} catch (Exception e) {
				log.warn("PageSpeed mobile audits unavailable for responsive check: {}", e.toString());
				notes.add("pagespeed overlay skipped");
			}
		}

		if (!usedPageSpeed) {
			addHtmlNotes(page.document(), issues, notes);
		}

		List<String> issueList = new ArrayList<>(issues);
		boolean ok = issueList.isEmpty();
		Map<String, String> explained = ok ? null : explainIssues(issueList);
		String evidence;
		if (ok) {
			evidence = notes.isEmpty() ? (usedPageSpeed ? "pagespeed mobile audits ok" : "html heuristics ok")
					: String.join("; ", notes);
		} else {
			evidence = "issues=" + issueList.size() + (usedPageSpeed ? " (html+pagespeed)" : " (html)");
		}
		double confidence = ok
				? (usedPageSpeed ? 0.92 : 0.85)
				: Math.max(0.35, 1.0 - (issueList.size() * 0.15));
		return ResponsiveValidationResultDto.builder()
				.isMobileFriendly(ok)
				.listOfIssues(explained)
				.confidence(confidence)
				.evidence(evidence)
				.build();
	}

	public static ResponsiveValidationResultDto analyzeHtml(String html) {
		return analyze(PageDocument.fromHtml("https://example.invalid/", html), null);
	}

	/**
	 * Overlay Lighthouse mobile SEO audits onto an HTML issue set.
	 *
	 * @return true when at least one relevant audit was found
	 */
	public static boolean applyLighthouseMobileAudits(String pagespeedJson, Set<String> issues, List<String> notes) {
		JsonObject audits = readAudits(pagespeedJson);
		if (audits == null || audits.size() == 0) {
			return false;
		}

		boolean sawRelevant = false;
		Boolean viewportPass = auditPassed(audits, "viewport");
		if (viewportPass != null) {
			sawRelevant = true;
			issues.remove(IssuesEnum.CONFIGURE_VIEWPORT.name());
			issues.remove(IssuesEnum.FIXED_WIDTH_VIEWPORT.name());
			if (viewportPass) {
				notes.add("pagespeed viewport ok");
			} else {
				issues.add(IssuesEnum.CONFIGURE_VIEWPORT.name());
				notes.add("pagespeed viewport failed");
			}
		}

		sawRelevant |= overlayAudit(audits, "content-width", IssuesEnum.SIZE_CONTENT_TO_VIEWPORT, issues, notes);
		sawRelevant |= overlayAudit(audits, "font-size", IssuesEnum.USE_LEGIBLE_FONT_SIZES, issues, notes);
		sawRelevant |= overlayAudit(audits, "tap-targets", IssuesEnum.TAP_TARGETS_TOO_CLOSE, issues, notes);
		return sawRelevant;
	}

	/**
	 * SPAs often omit {@code <meta viewport>} in the static shell but still render fine on mobile.
	 * If PageSpeed produced a mobile Lighthouse result and did not fail a viewport audit, drop the
	 * HTML-only CONFIGURE_VIEWPORT false positive.
	 */
	public static void softenMissingViewportMeta(String pagespeedJson, Set<String> issues, List<String> notes) {
		if (!issues.contains(IssuesEnum.CONFIGURE_VIEWPORT.name())) {
			return;
		}
		JsonObject audits = readAudits(pagespeedJson);
		Boolean viewportPass = audits == null ? null : auditPassed(audits, "viewport");
		if (Boolean.FALSE.equals(viewportPass)) {
			return;
		}
		if (Boolean.TRUE.equals(viewportPass)) {
			return; // already handled by applyLighthouseMobileAudits
		}
		// Audit missing/null: mobile lab ran, no explicit viewport failure → clear HTML-only miss.
		issues.remove(IssuesEnum.CONFIGURE_VIEWPORT.name());
		notes.add("pagespeed mobile lab ok; html viewport meta absent (spa soft-pass)");
	}

	private static boolean hasLighthouseResult(String pagespeedJson) {
		return readAudits(pagespeedJson) != null;
	}

	private static boolean overlayAudit(
			JsonObject audits,
			String auditId,
			IssuesEnum issue,
			Set<String> issues,
			List<String> notes) {
		Boolean pass = auditPassed(audits, auditId);
		if (pass == null) {
			return false;
		}
		issues.remove(issue.name());
		if (pass) {
			notes.add("pagespeed " + auditId + " ok");
		} else {
			issues.add(issue.name());
			notes.add("pagespeed " + auditId + " failed");
		}
		return true;
	}

	private static Boolean auditPassed(JsonObject audits, String auditId) {
		JsonObject audit = asObject(audits.get(auditId));
		if (audit == null || !audit.has("score") || audit.get("score").isJsonNull()) {
			return null;
		}
		return audit.get("score").getAsDouble() >= LIGHTHOUSE_PASS_SCORE;
	}

	private static JsonObject readAudits(String pagespeedJson) {
		try {
			JsonObject root = JsonParser.parseString(pagespeedJson).getAsJsonObject();
			JsonObject lighthouse = asObject(root.get("lighthouseResult"));
			if (lighthouse == null) {
				return null;
			}
			return asObject(lighthouse.get("audits"));
		} catch (Exception e) {
			return null;
		}
	}

	private static JsonObject asObject(JsonElement element) {
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	private static Set<String> collectHtmlIssues(Document document) {
		Set<String> issues = new LinkedHashSet<>();

		Element viewport = document.selectFirst("meta[name=viewport]");
		if (viewport == null || viewport.attr("content").isBlank()) {
			issues.add(IssuesEnum.CONFIGURE_VIEWPORT.name());
		} else {
			String content = viewport.attr("content").toLowerCase(Locale.ROOT);
			boolean hasDeviceWidth = content.contains("width=device-width");
			if (!hasDeviceWidth && FIXED_WIDTH.matcher(content).find()) {
				issues.add(IssuesEnum.FIXED_WIDTH_VIEWPORT.name());
			}
		}

		if (!document.select(
				"applet, object[type*=shockwave], embed[type*=shockwave], object[type*=flash], embed[type*=flash]")
				.isEmpty()) {
			issues.add(IssuesEnum.USES_INCOMPATIBLE_PLUGINS.name());
		}

		if (hasFixedWidthContentHint(document)) {
			issues.add(IssuesEnum.SIZE_CONTENT_TO_VIEWPORT.name());
		}

		if (hasIllegibleInlineFont(document)) {
			issues.add(IssuesEnum.USE_LEGIBLE_FONT_SIZES.name());
		}

		if (hasTightTapTargets(document)) {
			issues.add(IssuesEnum.TAP_TARGETS_TOO_CLOSE.name());
		}
		return issues;
	}

	private static void addHtmlNotes(Document document, Set<String> issues, List<String> notes) {
		if (!issues.contains(IssuesEnum.CONFIGURE_VIEWPORT.name())
				&& !issues.contains(IssuesEnum.FIXED_WIDTH_VIEWPORT.name())) {
			notes.add("viewport ok");
		}
		if (!issues.contains(IssuesEnum.USE_LEGIBLE_FONT_SIZES.name())) {
			notes.add("no tiny inline fonts found");
		}
		if (!issues.contains(IssuesEnum.TAP_TARGETS_TOO_CLOSE.name())) {
			notes.add("no tiny explicit tap targets found");
		}
	}

	public static Map<String, String> explainIssues(List<String> listOfIssues) {
		Map<String, String> explained = new LinkedHashMap<>();
		for (String issue : listOfIssues) {
			IssuesEnum value = IssuesEnum.valueOf(issue);
			explained.put(value.toString(), value.getValue());
		}
		return explained;
	}

	private static boolean hasFixedWidthContentHint(Document document) {
		Elements wide = document.select("[width], [style*=width]");
		int suspects = 0;
		for (Element el : wide) {
			String widthAttr = el.attr("width");
			if (WIDTH_HEIGHT_ATTR.matcher(widthAttr).matches()) {
				int px = Integer.parseInt(widthAttr);
				if (px >= MAX_CONTENT_WIDTH_HINT) {
					suspects++;
				}
			}
			String style = el.attr("style").toLowerCase(Locale.ROOT);
			Matcher m = Pattern.compile("width\\s*:\\s*(\\d+)px").matcher(style);
			if (m.find() && Integer.parseInt(m.group(1)) >= MAX_CONTENT_WIDTH_HINT) {
				suspects++;
			}
		}
		return suspects >= 3;
	}

	private static boolean hasIllegibleInlineFont(Document document) {
		for (Element el : document.select("[style*=font-size]")) {
			Matcher matcher = FONT_SIZE_PX.matcher(el.attr("style"));
			if (matcher.find()) {
				double px = Double.parseDouble(matcher.group(1));
				if (px > 0 && px < MIN_LEGIBLE_FONT_PX) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean hasTightTapTargets(Document document) {
		Elements targets = document.select("a, button, input, [role=button]");
		int tiny = 0;
		for (Element el : targets) {
			Integer w = dimensionPx(el, "width");
			Integer h = dimensionPx(el, "height");
			if (w != null && h != null && (w < MIN_TAP_TARGET_PX || h < MIN_TAP_TARGET_PX)) {
				tiny++;
			}
		}
		return tiny >= 3;
	}

	private static Integer dimensionPx(Element el, String name) {
		String attr = el.attr(name);
		if (WIDTH_HEIGHT_ATTR.matcher(attr).matches()) {
			return Integer.parseInt(attr);
		}
		Matcher matcher = Pattern.compile("(?i)" + name + "\\s*:\\s*(\\d+)px").matcher(el.attr("style"));
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		return null;
	}

	@Getter
	public enum IssuesEnum {

		MOBILE_FRIENDLY_RULE_UNSPECIFIED("Unspecified mobile-friendly issue."),
		USES_INCOMPATIBLE_PLUGINS("Plugins incompatible with mobile devices are being used."),
		CONFIGURE_VIEWPORT("Viewport is not specified using the meta viewport tag."),
		FIXED_WIDTH_VIEWPORT("Viewport defined to a fixed width."),
		SIZE_CONTENT_TO_VIEWPORT("Content appears sized wider than a typical mobile viewport."),
		USE_LEGIBLE_FONT_SIZES("Inline font size is too small for easy reading on a small screen."),
		TAP_TARGETS_TOO_CLOSE("Interactive elements declare dimensions that are likely too small to tap.");

		private final String value;

		IssuesEnum(String value) {
			this.value = value;
		}
	}
}

package br.com.uhunter.validations.performance;

import java.io.IOException;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.com.uhunter.pagespeed.PageSpeedInsightsClient;
import br.com.uhunter.validations.performance.PerformanceMetric.PerformanceMetricValues;
import lombok.Getter;

/**
 * Uses PageSpeed Insights API v5.
 */
public class PerformanceAnalysis {

	private final PageSpeedInsightsClient pageSpeedInsightsClient;
	@Getter
	private String jsonString;
	private PerformanceMetric fcp;
	private PerformanceMetric lcp;
	@Getter
	private String loadingOverall;
	@Getter
	private int optimizationPoints;

	public PerformanceAnalysis(String url, boolean isMobile, PageSpeedInsightsClient pageSpeedInsightsClient)
			throws IOException {
		this.pageSpeedInsightsClient = Objects.requireNonNull(pageSpeedInsightsClient, "pageSpeedInsightsClient");
		doTest(url, isMobile);
		setFCP();
		setLCP();
		setOptimizationPoints();
		setLoadingOverall();
	}

	public String doTest(String url, boolean isMobile) throws IOException {
		String responseBody = pageSpeedInsightsClient.getOrFetch(url, isMobile);
		setJsonString(responseBody);
		return responseBody;
	}

	public void setFCP() {
		fcp = getMetricJsonToObject(PerformanceMetricValues.FIRST_CONTENTFUL_PAINT_MS, "first-contentful-paint");
	}

	public PerformanceMetric getFCP() {
		return fcp;
	}

	public void setLCP() {
		lcp = getMetricJsonToObject(PerformanceMetricValues.LARGEST_CONTENTFUL_PAINT_MS, "largest-contentful-paint");
	}

	public PerformanceMetric getLCP() {
		return lcp;
	}

	public PerformanceMetric getMetricJsonToObject(PerformanceMetricValues typeMetric, String lighthouseAuditId) {
		PerformanceMetric performanceMetric = new PerformanceMetric();
		performanceMetric.setType(typeMetric);
		performanceMetric.setDescription(typeMetric.getValue());

		JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
		JsonObject fieldMetric = getFieldMetric(root, typeMetric.name());

		if (fieldMetric != null) {
			performanceMetric.setPageSpeedMetric(readPercentileMs(fieldMetric));
			if (fieldMetric.has(PerformanceMetricValues.CATEGORY.getValue())) {
				performanceMetric.setOverallMetric(fieldMetric.get(PerformanceMetricValues.CATEGORY.getValue()).getAsString());
			}
			applyDistributions(performanceMetric, fieldMetric);
			return performanceMetric;
		}

		// Fallback to Lighthouse lab data when CrUX field data is missing.
		JsonObject audit = getLighthouseAudit(root, lighthouseAuditId);
		if (audit != null && audit.has("numericValue")) {
			int ms = (int) Math.round(audit.get("numericValue").getAsDouble());
			performanceMetric.setPageSpeedMetric(ms);
			performanceMetric.setOverallMetric(categoryFromMs(typeMetric, ms));
			performanceMetric.setFast(null);
			performanceMetric.setAverage(null);
			performanceMetric.setSlow(null);
		}

		return performanceMetric;
	}

	public void setLoadingOverall() {
		JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
		JsonObject loadingExperience = asObject(root.get(PerformanceMetricValues.LOADING_EXPERIENCE.getValue()));
		if (loadingExperience != null && loadingExperience.has(PerformanceMetricValues.OVERALL_CATEGORY.getValue())) {
			this.loadingOverall = loadingExperience.get(PerformanceMetricValues.OVERALL_CATEGORY.getValue()).getAsString();
			return;
		}

		Double score = getLighthousePerformanceScore(root);
		if (score == null) {
			this.loadingOverall = "UNKNOWN";
		} else if (score >= 0.9) {
			this.loadingOverall = "FAST";
		} else if (score >= 0.5) {
			this.loadingOverall = "AVERAGE";
		} else {
			this.loadingOverall = "SLOW";
		}
	}

	public void setOptimizationPoints() {
		JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
		Double score = getLighthousePerformanceScore(root);
		// Thesis used 0-100 "how much improvement is needed"; invert Lighthouse score.
		this.optimizationPoints = score == null ? -1 : (int) Math.round((1.0 - score) * 100.0);
	}

	public void setJsonString(String jsonString) {
		this.jsonString = jsonString;
	}

	private static JsonObject getFieldMetric(JsonObject root, String metricName) {
		JsonObject loadingExperience = asObject(root.get(PerformanceMetricValues.LOADING_EXPERIENCE.getValue()));
		if (loadingExperience == null) {
			return null;
		}
		JsonObject metrics = asObject(loadingExperience.get(PerformanceMetricValues.METRICS.getValue()));
		if (metrics == null) {
			return null;
		}
		return asObject(metrics.get(metricName));
	}

	private static JsonObject getLighthouseAudit(JsonObject root, String auditId) {
		JsonObject lighthouse = asObject(root.get("lighthouseResult"));
		if (lighthouse == null) {
			return null;
		}
		JsonObject audits = asObject(lighthouse.get("audits"));
		if (audits == null) {
			return null;
		}
		return asObject(audits.get(auditId));
	}

	private static Double getLighthousePerformanceScore(JsonObject root) {
		JsonObject lighthouse = asObject(root.get("lighthouseResult"));
		if (lighthouse == null) {
			return null;
		}
		JsonObject categories = asObject(lighthouse.get("categories"));
		if (categories == null) {
			return null;
		}
		JsonObject performance = asObject(categories.get("performance"));
		if (performance == null || !performance.has("score") || performance.get("score").isJsonNull()) {
			return null;
		}
		return performance.get("score").getAsDouble();
	}

	private static void applyDistributions(PerformanceMetric performanceMetric, JsonObject fieldMetric) {
		if (!fieldMetric.has(PerformanceMetricValues.DISTRIBUTIONS.getValue())) {
			return;
		}
		JsonArray distributions = fieldMetric.get(PerformanceMetricValues.DISTRIBUTIONS.getValue()).getAsJsonArray();
		if (distributions.size() > 0) {
			performanceMetric.setFast(readProportion(distributions.get(0)));
		}
		if (distributions.size() > 1) {
			performanceMetric.setAverage(readProportion(distributions.get(1)));
		}
		if (distributions.size() > 2) {
			performanceMetric.setSlow(readProportion(distributions.get(2)));
		}
	}

	private static Double readProportion(JsonElement element) {
		JsonObject obj = asObject(element);
		if (obj == null || !obj.has(PerformanceMetricValues.PROPORTION.getValue())) {
			return null;
		}
		return obj.get(PerformanceMetricValues.PROPORTION.getValue()).getAsDouble();
	}

	private static Integer readPercentileMs(JsonObject fieldMetric) {
		if (fieldMetric.has(PerformanceMetricValues.PERCENTILE.getValue())) {
			return fieldMetric.get(PerformanceMetricValues.PERCENTILE.getValue()).getAsInt();
		}
		if (fieldMetric.has("median")) {
			return fieldMetric.get("median").getAsInt();
		}
		return null;
	}

	private static String categoryFromMs(PerformanceMetricValues type, int ms) {
		if (type == PerformanceMetricValues.FIRST_CONTENTFUL_PAINT_MS) {
			if (ms <= 1800) {
				return "FAST";
			}
			if (ms <= 3000) {
				return "AVERAGE";
			}
			return "SLOW";
		}
		if (ms <= 2500) {
			return "FAST";
		}
		if (ms <= 4000) {
			return "AVERAGE";
		}
		return "SLOW";
	}

	private static JsonObject asObject(JsonElement element) {
		if (element == null || element.isJsonNull() || !element.isJsonObject()) {
			return null;
		}
		return element.getAsJsonObject();
	}
}

package br.com.uhunter.validations.performance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PerformanceMetric {

	private PerformanceMetricValues type;
	private String description;
	private Integer pageSpeedMetric;
	private Double fast;
	private Double average;
	private Double slow;
	private String overallMetric;

	@Getter
	public enum PerformanceMetricValues {

		LOADING_EXPERIENCE("loadingExperience"),
		METRICS("metrics"),

		FIRST_CONTENTFUL_PAINT_MS("FIRST_CONTENTFUL_PAINT_MS - First Contentful Paint (field/lab)"),
		LARGEST_CONTENTFUL_PAINT_MS("LARGEST_CONTENTFUL_PAINT_MS - Largest Contentful Paint (field/lab)"),

		PERCENTILE("percentile"),
		DISTRIBUTIONS("distributions"),
		CATEGORY("category"),
		PROPORTION("proportion"),
		OVERALL_CATEGORY("overall_category");

		private final String value;

		PerformanceMetricValues(String value) {
			this.value = value;
		}
	}
}

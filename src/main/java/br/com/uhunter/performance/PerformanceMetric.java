package br.com.uhunter.performance;

public class PerformanceMetric {
	
	private PerformanceMetricValues type;
	private String description;
	private Integer pageSpeedMetric;
	private Double fast;
	private Double average;
	private Double slow;
	private String overallMetric;
	
	public PerformanceMetric() {
		
	}
	
	public PerformanceMetricValues getType() {
		return type;
	}

	public void setType(PerformanceMetricValues firstContentfulPaintMs) {
		this.type = firstContentfulPaintMs;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
		
	public Integer getPageSpeedMetric() {
		return pageSpeedMetric;
	}

	public void setPageSpeedMetric(Integer pageSpeedMetric) {
		this.pageSpeedMetric = pageSpeedMetric;
	}

	public Double getFast() {
		return fast;
	}

	public void setFast(Double fast) {
		this.fast = fast;
	}

	public Double getAverage() {
		return average;
	}

	public void setAverage(Double average) {
		this.average = average;
	}

	public Double getSlow() {
		return slow;
	}

	public void setSlow(Double slow) {
		this.slow = slow;
	}

	public String getOverallMetric() {
		return overallMetric;
	}

	public void setOverallMetric(String overallMetric) {
		this.overallMetric = overallMetric;
	}

	public enum PerformanceMetricValues {
		
		LOADING_EXPERIENCE("loadingExperience"),
		METRICS("metrics"),
		
		FIRST_CONTENTFUL_PAINT_MS("FIRST_CONTENTFUL_PAINT_MS - Description for this type of test"),
		MEDIAN("median"),
		DISTRIBUTIONS("distributions"),
		CATEGORY("category"),		
		
		MIN("min"),
		MAX("max"),
		PROPORTION("proportion"),
		
		DOM_CONTENT_LOADED_EVENT_FIRED_MS("DOM_CONTENT_LOADED_EVENT_FIRED_MS - Description for this type of test"),
		
		OVERALL_CATEGORY("overall_category"), 
		
		RULE_GROUPS("ruleGroups"),
		SPEED("SPEED"), 
		SCORE("score");
		
		private String value;
		
		PerformanceMetricValues(String value){
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}

	}
	
}

package br.com.uhunter.performance;

import java.util.List;
import java.util.Map;

public class PerformanceMetric {
	
	private String type;
	private int median;
	private List<Map<String, Double>> distributions;
	private String category;
	
	public enum PerformanceMetricValues {
		
		LOADING_EXPERIENCE("loadingExperience"),
		METRICS("metrics"),
		
		FIRST_CONTENTFUL_PAINT("FIRST_CONTENTFUL_PAINT_MS"),		
		MEDIAN("median"),
		DISTRIBUTIONS("distributions"),
		CATEGORY("category"),		
		
		MIN("min"),
		MAX("max"),
		PROPORTION("proportion"),
		
		DOM_CONTENT_LOADED_EVENT_FIRED_MS("DOM_CONTENT_LOADED_EVENT_FIRED_MS"),
		
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

	public void setType(String type) {
		this.type = type;		
	}
	
	public String getType() {
		return type;
	}

	public void setMedian(int median) {
        this.median = median;		
	}
	
	public int getMedian() {
		return median;
	}

	public void setDistributions(List<Map<String, Double>> distributions) {
		this.distributions = distributions;		
	}
	
	public List<Map<String, Double>> getDistributions() {
		return distributions;
	}

	public void setCategory(String category) {
		this.category = category;		
	}
	
	public String getCategory() {
		return category;
	}

}

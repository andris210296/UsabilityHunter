package br.com.uhunter.utils;

public enum JsonValues {
	
	API_KEY("AIzaSyC4AqjLD8DJsxBnPkQTjAcGHqG7B0XI5HE"),
	
	LOGO_ON_TOP_LEFT("logoOnTopLeft"),
	LOGO_NAME("logoName"),
	
	RESULT("result"),
	
	NAVIGATION_ON_LEFT_CORNER_TEST("navigationOnLeftCornerTest"),
	NAVIGATION_ON_LEFT_CORNER("navigationOnLeftCorner"),
	HICKSLAW("hicksLaw"),
	
	NOT_APPLICABLE("not applicable"),
	
	IS_MOBILE_FRIENDLY_TEST("isMobileFriendlyTest"),
	IS_MOBILE_FRIENDLY("isMobileFriendly"),
	LIST_OF_ISSUES("listOfIssues"),
	
	PERFORMANCE_TEST("performanceTest"),
	DESKTOP_TEST("desktopTest"),
	MOBILE_TEST("mobileTest"),
	OPTIMIZATION_POINTS("optimizationPoints"),
	LOADING_OVERALL("loadingOverall"),
		
	LOADING_PERFORMANCE("loadingPerformance"),
	
	METRICS("metrics"),
	
	TYPE("type"),
	DESCRIPTION("description"),
	PAGE_SPEED("pageSpeed"),
	PAGE_CONTENT_LOAD_DISTRIBUTION_AVERAGE("pageContentLoadDistributionAverage"),
	PAGE_CONTENT_LOAD_DISTRIBUTION("pageContentLoadDistribution"),
	
	FAST("fast"),
	AVERAGE("average"),
	SLOW("slow");
	
	private String value;
	
	JsonValues(String value){
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}

}

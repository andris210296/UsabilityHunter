package br.com.uhunter;

public enum JsonValues {
	
	LOGO_ON_TOP_LEFT("logoOnTopLeft"),
	LOGO_NAME("logoName"),
	
	RESULT("result"),
	
	NAVIGATION_ON_LEFT_CORNER_TEST("navigationOnLeftCornerTest"),
	NAVIGATION_ON_LEFT_CORNER("navigationOnLeftCorner"),
	HICKSLAW("hicksLaw"),
	
	NOT_APPLICABLE("not applicable"),
	
	IS_MOBILE_FRIENDLY_TEST("isMobileFriendlyTest"),
	IS_MOBILE_FRIENDLY("isMobileFriendly"),
	LIST_OF_ISSUES("listOfIssues");
	
	public String value;
	
	JsonValues(String value){
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}

}

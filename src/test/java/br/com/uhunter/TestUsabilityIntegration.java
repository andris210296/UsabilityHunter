package br.com.uhunter;

import static org.junit.Assert.*;

import java.util.*;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.repackaged.com.google.gson.JsonObject;
import com.google.gson.Gson;

import br.com.uhunter.performance.PerformanceMetric;
import br.com.uhunter.performance.PerformanceMetric.PerformanceMetricValues;
import br.com.uhunter.responsive.IsMobileFriendly.IssuesEnum;
import br.com.uhunter.rest.UsabilityIntegration;
import br.com.uhunter.utils.JsonValues;

public class TestUsabilityIntegration {
	
	private static final String URL_1 = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";
	private static final String URL_2 = "https://pt.stackoverflow.com/";
	private static final String URL_3 = "https://cursosextensao.usp.br/course/view.php?id=131&section=0";
	private static final String URL_4 = "https://www.vestibularfatec.com.br/home/";
	
	
	private UsabilityIntegration usabilityIntegration1;
	private UsabilityIntegration usabilityIntegration2;
	private UsabilityIntegration usabilityIntegration3;
	private UsabilityIntegration usabilityIntegration4;
			
	@Before
	public void initialConfig() throws Exception {
		usabilityIntegration1 = new UsabilityIntegration(URL_1);
		usabilityIntegration2 = new UsabilityIntegration(URL_2);
		usabilityIntegration3 = new UsabilityIntegration(URL_3);
		usabilityIntegration4 = new UsabilityIntegration(URL_4);
	}
	
	@Test
	public void TestLogoOnTopLeftCorner() throws Exception {
					
		Map mapExpected1 = populateLogoMap("Wikipedia");		
		Map mapResult1 = usabilityIntegration1.doLogoTest();		
		assertEquals(mapExpected1, mapResult1);	
					
		Map mapExpected2 = populateLogoMap("StackOverflow");	
		Map mapResult2 = usabilityIntegration2.doLogoTest();		
		assertEquals(mapExpected2, mapResult2);		
		
	}
	
	private Map<String, Object> populateLogoMap(String logo) {
		Map<String, Object> map = new HashMap<>();
        map.put(JsonValues.LOGO_NAME.getValue(), logo);
        map.put(JsonValues.RESULT.getValue(), true);
        return map;
	}
	
	@Test
	public void TestNavigationOnLeftCorner() throws Exception {

		Map<String, Object> mapExpected1 = populateNavigationMap(true, false);		
		Map mapResult1 = usabilityIntegration1.doNavigationOnLeftCornerTest();
		assertEquals(mapExpected1, mapResult1);
		
		Map<String, Object> mapExpected2 = populateNavigationMap(false, JsonValues.NOT_APPLICABLE.getValue());
		Map<String, Object> mapResult2 = usabilityIntegration2.doNavigationOnLeftCornerTest();
		assertEquals(mapExpected2, mapResult2);		
		
		Map<String, Object> mapExpected3 = populateNavigationMap(true, true);
		Map<String, Object> mapResult3 = usabilityIntegration3.doNavigationOnLeftCornerTest();
		assertEquals(mapExpected3, mapResult3);				
	}
	
	private Map<String, Object> populateNavigationMap(boolean result, Object hickslaw) {
		Map<String, Object> map = new HashMap<>();
        map.put(JsonValues.NAVIGATION_ON_LEFT_CORNER.getValue(), result);
        map.put(JsonValues.HICKSLAW.getValue(), hickslaw);
        return map;
	}
	
	@Test
	public void TestIsMobileFriendly() {
		
		Map<String, Object> mapsExpected1 = populateIsMobileFriendlyMap(true, new HashMap());		
		Map<String, Object> mapResult1 = usabilityIntegration1.doIsMobileFriendlyTest();
		assertEquals(mapsExpected1, mapResult1);
		
		
		Map<String,String> listOfIssues = new HashMap<>();
		listOfIssues.put(IssuesEnum.USE_LEGIBLE_FONT_SIZES.toString(), IssuesEnum.USE_LEGIBLE_FONT_SIZES.getValue());
		listOfIssues.put(IssuesEnum.CONFIGURE_VIEWPORT.toString(), IssuesEnum.CONFIGURE_VIEWPORT.getValue());
		listOfIssues.put(IssuesEnum.SIZE_CONTENT_TO_VIEWPORT.toString(), IssuesEnum.SIZE_CONTENT_TO_VIEWPORT.getValue());
		listOfIssues.put(IssuesEnum.TAP_TARGETS_TOO_CLOSE.toString(), IssuesEnum.TAP_TARGETS_TOO_CLOSE.getValue());		
		
		Map<String, Object> mapsExpected2 = populateIsMobileFriendlyMap(false, listOfIssues);	
		Map<String, Object> mapResult2 = usabilityIntegration4.doIsMobileFriendlyTest();
		assertEquals(mapsExpected2, mapResult2);
	}
	
	private Map<String, Object> populateIsMobileFriendlyMap(boolean result, Map<String, String> listOfIssues) {
		Map<String, Object> map = new HashMap<>();
		
		map.put(JsonValues.IS_MOBILE_FRIENDLY.getValue(), result);
        
		if (!listOfIssues.isEmpty()) {
			map.put(JsonValues.LIST_OF_ISSUES.getValue(), listOfIssues);		
		}
		
        return map;
	}
	
	@Test
	public void TestPerformance() {		
		Map<String, Object> mapResult1 = usabilityIntegration1.doPerformanceTest();
		assertTrue(mapResult1.get(JsonValues.PERFORMANCE_TEST.getValue()).toString().contains(PerformanceMetricValues.FIRST_CONTENTFUL_PAINT_MS.getValue()));
		assertTrue(mapResult1.get(JsonValues.PERFORMANCE_TEST.getValue()).toString().contains(PerformanceMetricValues.DOM_CONTENT_LOADED_EVENT_FIRED_MS.getValue()));
		assertTrue(mapResult1.get(JsonValues.PERFORMANCE_TEST.getValue()).toString().contains(JsonValues.MOBILE_TEST.getValue()));
		assertTrue(mapResult1.get(JsonValues.PERFORMANCE_TEST.getValue()).toString().contains(JsonValues.DESKTOP_TEST.getValue()));
	}
}

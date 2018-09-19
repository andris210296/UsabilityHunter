package br.com.uhunter;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

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
	
	private Map<String, String> populateLogoMap(String logo) {
		Map<String, String> map = new HashMap<>();
        map.put(JsonValues.LOGO_NAME.getValue(), logo);
        map.put(JsonValues.RESULT.getValue(), String.valueOf(true));
        return map;
	}
	
	@Test
	public void TestNavigationOnLeftCorner() throws Exception {

		Map<String, String> mapExpected1 = populateNavigationMap(true, String.valueOf(false));		
		Map mapResult1 = usabilityIntegration1.doNavigationOnLeftCornerTest();
		assertEquals(mapExpected1, mapResult1);
		
		Map<String, String> mapExpected2 = populateNavigationMap(false, JsonValues.NOT_APPLICABLE.getValue());
		Map<String, String> mapResult2 = usabilityIntegration2.doNavigationOnLeftCornerTest();
		assertEquals(mapExpected2, mapResult2);		
		
		Map<String, String> mapExpected3 = populateNavigationMap(true, String.valueOf(true));
		Map<String, String> mapResult3 = usabilityIntegration3.doNavigationOnLeftCornerTest();
		assertEquals(mapExpected3, mapResult3);				
	}
	
	private Map<String, String> populateNavigationMap(boolean result, String hickslaw) {
		Map<String, String> map = new HashMap<>();
        map.put(JsonValues.NAVIGATION_ON_LEFT_CORNER.getValue(), String.valueOf(result));
        map.put(JsonValues.HICKSLAW.getValue(), String.valueOf(hickslaw));
        return map;
	}
	
	@Test
	public void TestIsMobileFriendly() {
		
		List<Map> mapsExpected1 = populateIsMobileFriendlyMap(true, new HashMap());		
		List<Map> mapResult1 = usabilityIntegration1.doIsMobileFriendlyTest();
		assertEquals(mapsExpected1, mapResult1);
		
		
		Map<String,String> listOfIssues = new HashMap<>();
		listOfIssues.put(IssuesEnum.USE_LEGIBLE_FONT_SIZES.toString(), IssuesEnum.USE_LEGIBLE_FONT_SIZES.getValue());
		listOfIssues.put(IssuesEnum.CONFIGURE_VIEWPORT.toString(), IssuesEnum.CONFIGURE_VIEWPORT.getValue());
		listOfIssues.put(IssuesEnum.SIZE_CONTENT_TO_VIEWPORT.toString(), IssuesEnum.SIZE_CONTENT_TO_VIEWPORT.getValue());
		listOfIssues.put(IssuesEnum.TAP_TARGETS_TOO_CLOSE.toString(), IssuesEnum.TAP_TARGETS_TOO_CLOSE.getValue());		
		
		List<Map> mapsExpected2 = populateIsMobileFriendlyMap(false, listOfIssues);	
		List<Map> mapResult2 = usabilityIntegration4.doIsMobileFriendlyTest();
		assertEquals(mapsExpected2, mapResult2);
	}
	
	private List<Map> populateIsMobileFriendlyMap(boolean result, Map<String, String> listOfIssues) {
		List<Map> maps = new ArrayList<>();
		Map<String, String> mapValue = new HashMap<>();
		Map<String, Map<String, String>> mapIssues = new HashMap<>();
		
        mapValue.put(JsonValues.IS_MOBILE_FRIENDLY.getValue(), String.valueOf(result));
        maps.add(mapValue);
        
		if (!listOfIssues.isEmpty()) {
			mapIssues.put(JsonValues.LIST_OF_ISSUES.getValue(), listOfIssues);
			maps.add(mapIssues);			
		}
		
        return maps;
	}

}

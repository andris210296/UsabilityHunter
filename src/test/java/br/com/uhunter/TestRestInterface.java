package br.com.uhunter;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.json.JSONObject;
import org.junit.Test;

public class TestRestInterface {
	
	private static final String URL_1 = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";
	private static final String URL_2 = "https://pt.stackoverflow.com/";
	private static final String URL_3 = "https://cursosextensao.usp.br/course/view.php?id=131&section=0";
	private static final String URL_4 = "https://www.vestibularfatec.com.br/home/";
			
	@Test
	public void TestRestInterface() {
		
		JSONObject jsonExpected1 = new JSONObject();
		jsonExpected1.put(JsonValues.LOGO_ON_TOP_LEFT.getValue(), logoIdentification(true, URL_1));
		jsonExpected1.put(JsonValues.NAVIGATION_ON_LEFT_CORNER_TEST.getValue(), populateNavigationMap(true, String.valueOf(false)));
		jsonExpected1.put(JsonValues.IS_MOBILE_FRIENDLY_TEST.getValue(), populateIsMobileFriendlyMap(true, new HashMap()));
		
		JSONObject url1 = new JSONObject();
		url1.put("url",URL_1);
		
		RestInterfaceImpl usabilityRestImpl1 = new RestInterfaceImpl();
		String result1 =  usabilityRestImpl1.setTest(url1.toString());
		
		assertEquals(jsonExpected1.toString(), result1);
	}

	private Map<String, String> logoIdentification(boolean result, String url) {
		Map<String, String> map = new HashMap<>();
        map.put(JsonValues.LOGO_NAME.getValue(), url);
        map.put(JsonValues.RESULT.getValue(), String.valueOf(result));
        return map;
	}

	private Map<String, String> populateNavigationMap(boolean result, String hickslaw) {
		Map<String, String> map = new HashMap<>();
        map.put(JsonValues.NAVIGATION_ON_LEFT_CORNER.getValue(), String.valueOf(result));
        map.put(JsonValues.HICKSLAW.getValue(), String.valueOf(hickslaw));
        return map;
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

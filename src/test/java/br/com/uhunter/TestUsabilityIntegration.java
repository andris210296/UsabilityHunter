package br.com.uhunter;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class TestUsabilityIntegration {
	
	private static final String URL_1 = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";
	private static final String URL_2 = "https://pt.stackoverflow.com/";
	private static final String URL_3 = "https://cursosextensao.usp.br/course/view.php?id=131&section=0";
	
	private UsabilityIntegration usabilityIntegration1;
	private UsabilityIntegration usabilityIntegration2;
	private UsabilityIntegration usabilityIntegration3;
		
	
	@Before
	public void initialConfig() throws Exception {
		usabilityIntegration1 = new UsabilityIntegration(URL_1);
		usabilityIntegration2 = new UsabilityIntegration(URL_2);
		usabilityIntegration3 = new UsabilityIntegration(URL_3);
	}
	
	@Test
	public void TestLogoOnTopLeftCorner() throws Exception {
					
		JSONObject jsonExpected1 = populateLogoJson("Wikipedia");		
		JSONObject jsonResult1 = usabilityIntegration1.doLogoTest();		
		assertEquals(jsonExpected1.toString(), jsonResult1.toString());	
					
		JSONObject jsonExpected2 = populateLogoJson("StackOverflow");	
		JSONObject jsonResult2 = usabilityIntegration2.doLogoTest();		
		assertEquals(jsonExpected2.toString(), jsonResult2.toString());		
		
	}

	private JSONObject populateLogoJson(String logo) {
		JSONObject jsonExpected = new JSONObject();
		jsonExpected.put("logoName", logo);
		jsonExpected.put("result", true);
		return jsonExpected;
	}
	
	@Test
	public void TestNavigationOnLeftCorner() throws Exception {

		JSONObject jsonExpected1 = new JSONObject();
		jsonExpected1.put("navigationOnLeftCorner", true);
		jsonExpected1.put("hicksLaw", false);
		JSONObject jsonResult1 = usabilityIntegration1.doNavigationOnLeftCornerTest();
		assertEquals(jsonExpected1.toString(), jsonResult1.toString());
		
		JSONObject jsonExpected2 = new JSONObject();
		jsonExpected2.put("navigationOnLeftCorner", false);
		jsonExpected2.put("hicksLaw", "not applicable");
		JSONObject jsonResult2 = usabilityIntegration2.doNavigationOnLeftCornerTest();
		assertEquals(jsonExpected2.toString(), jsonResult2.toString());		
		
		JSONObject jsonExpected3 = new JSONObject();
		jsonExpected3.put("navigationOnLeftCorner", true);
		jsonExpected3.put("hicksLaw", true);
		JSONObject jsonResult3 = usabilityIntegration3.doNavigationOnLeftCornerTest();
		assertEquals(jsonExpected3.toString(), jsonResult3.toString());		
		
	}

}

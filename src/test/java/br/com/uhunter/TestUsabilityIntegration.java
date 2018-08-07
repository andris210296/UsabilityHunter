package br.com.uhunter;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Test;

public class TestUsabilityIntegration {
	
	@Test
	public void TestLogoOnTopLeftCorner() throws Exception {
		
		String url1 = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";		
		JSONObject jsonExpected1 = populateLogoJson("Wikipedia");		
		UsabilityIntegration usabilityIntegration1 = new UsabilityIntegration(url1);		
		JSONObject jsonResult1 = usabilityIntegration1.doLogoTest();		
		assertEquals(jsonExpected1.toString(), jsonResult1.toString());	
				
		String url2 = "https://pt.stackoverflow.com/";		
		JSONObject jsonExpected2 = populateLogoJson("StackOverflow");		
		UsabilityIntegration usabilityIntegration2 = new UsabilityIntegration(url2);		
		JSONObject jsonResult2 = usabilityIntegration2.doLogoTest();		
		assertEquals(jsonExpected2.toString(), jsonResult2.toString());
		
		
	}

	private JSONObject populateLogoJson(String logo) {
		JSONObject jsonExpected = new JSONObject();
		jsonExpected.put("logoName", logo);
		jsonExpected.put("result", true);
		return jsonExpected;
	}

}

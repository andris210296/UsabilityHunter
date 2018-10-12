package br.com.uhunter.rest;

import org.json.JSONObject;

import com.google.appengine.api.ThreadManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import br.com.uhunter.utils.JsonValues;

import java.lang.reflect.Type;
import java.util.*;

import javax.ws.rs.*;

@Path("/urest/")
public class RestInterfaceImpl {

	/**
	 * http://usabilityhunter.appspot.com/uhunter/urest/runtest
	 * http://usabilityhunter.appspot.com/rest/urest/runtest
	 * 
	 * @param content
	 * @return
	 */
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	@Path("/runtest")
	public String setTest(String content){
		Gson gson = new Gson();
		try {

			Type type = new TypeToken<Map<String, String>>() {
			}.getType();

			Map<String, String> map = gson.fromJson(content, type);
			String url = map.get("url");

			JSONObject jsonObject = new JSONObject();

			UsabilityIntegration usabilityIntegration = new UsabilityIntegration(url);

			Thread threadLogo = ThreadManager.createThreadForCurrentRequest(new Runnable() {
				public void run() {
					System.out.println("Logo");
					jsonObject.put(JsonValues.LOGO_ON_TOP_LEFT.getValue(), usabilityIntegration.doLogoTest());
				}
			});
			
			Thread threadNavigation = ThreadManager.createThreadForCurrentRequest(new Runnable() {
				public void run() {
					System.out.println("Navigation");
					jsonObject.put(JsonValues.NAVIGATION_ON_LEFT_CORNER_TEST.getValue(), usabilityIntegration.doNavigationOnLeftCornerTest());
				}
			});
			
			Thread threadMobile = ThreadManager.createThreadForCurrentRequest(new Runnable() {
				public void run() {
					System.out.println("Mobile");
					jsonObject.put(JsonValues.IS_MOBILE_FRIENDLY_TEST.getValue(), usabilityIntegration.doIsMobileFriendlyTest());
				}
			});
			
			Thread threadPerformance = ThreadManager.createThreadForCurrentRequest(new Runnable() {
				public void run() {
					System.out.println("Performance");
					jsonObject.put(JsonValues.PERFORMANCE_TEST.getValue(), usabilityIntegration.doPerformanceTest());
				}
			});
			
			//threadLogo.start();
			threadNavigation.start();
			//threadMobile.start();				
			//threadPerformance.start();
								
			
			while(jsonObject.keySet().size() != 1) {
			}

			return jsonObject.toString();

		} catch (Exception e) {
			e.printStackTrace();
			return gson.toJson("ServerResponse: Error: " + e.getMessage());

		}

	}

	@GET
	@Produces("application/json")
	@Path("/gettest")
	public String getTest() {
		// TODO Auto-generated method stub
		return null;
	}

}

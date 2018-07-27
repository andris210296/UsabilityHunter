package br.com.uhunter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.*;

public class IsMobileFriendly {
	
	private static final String API_URL = "https://searchconsole.googleapis.com/v1/urlTestingTools/mobileFriendlyTest:run?key=AIzaSyC4AqjLD8DJsxBnPkQTjAcGHqG7B0XI5HE";
	
	private static final String MOBILE_FRIENDLY = "MOBILE_FRIENDLY";
	
	private List<String> listOfIssues = new ArrayList<>();
	
	private boolean result;

	public IsMobileFriendly(String url) throws Exception {
		String response = doTest(url);
		httpResulToJson(response);
	}
	
	public IsMobileFriendly() {
		
	}

	private String doTest(String url) throws Exception {		
		HttpTransport httpTransport = new NetHttpTransport();
		HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
		GenericUrl urlApi = new GenericUrl(API_URL);
		JSONObject json = new JSONObject();
		json.put("url", url);
		json.put("requestScreenshot", "false");
		String requestBody = json.toString();
		HttpRequest request = requestFactory.buildPostRequest(urlApi,ByteArrayContent.fromString("application/json", requestBody));
		HttpResponse httpResponse = request.execute();

		return httpResponse.parseAsString();		
	}
	
	
	public void httpResulToJson(String stringJsonResult) {
		JsonObject jsonResult = new JsonParser().parse(stringJsonResult).getAsJsonObject();
		setResult(MOBILE_FRIENDLY.matches(jsonResult.get("mobileFriendliness").getAsString()));
		
		if(jsonResult.get("mobileFriendlyIssues") != null) {
			setListOfIssues(jsonResult.get("mobileFriendlyIssues").getAsJsonArray());
		}
	}
	
	private void setListOfIssues(JsonArray jsonArray) {
		for(int i = 0; i< jsonArray.size(); i++) {
			JsonObject element = jsonArray.get(i).getAsJsonObject();
			listOfIssues.add(element.get("rule").getAsString());
		}		
	}

	public boolean getResult() {
		return result;
	}
	
	private void setResult(boolean result) {
		this.result = result;
	}
	
	public List<String> getListOfIssues() {
		return listOfIssues;
	}

}

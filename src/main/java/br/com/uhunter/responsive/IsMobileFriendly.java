package br.com.uhunter.responsive;

import java.util.*;

import org.json.JSONObject;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.*;

import br.com.uhunter.utils.JsonValues;

public class IsMobileFriendly {
	
	private static final String API_URL = "https://searchconsole.googleapis.com/v1/urlTestingTools/mobileFriendlyTest:run?key=" + JsonValues.API_KEY.getValue();
	
	private static final String MOBILE_FRIENDLY = "MOBILE_FRIENDLY";
	
	private List<String> listOfIssues = new ArrayList<>();
	private Map<String, String> listOfIssuesExplained = new HashMap<String, String>();
	
	private boolean result;

	public IsMobileFriendly(String url) throws Exception {
		String response = doTest(url);
		httpResulToJson(response);
		setListOfIssuesExplained();
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
		HttpRequest request = requestFactory.buildPostRequest(urlApi,ByteArrayContent.fromString("application/json", requestBody))
				.setConnectTimeout(3 * 60000).setReadTimeout(3 * 60000);
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
	
	public void setListOfIssuesExplained() {
		Map<String, String> listOfIssuesExplained = new HashMap<>();
		
		if(!getListOfIssues().isEmpty()) {
			for (String issue : listOfIssues) {
				listOfIssuesExplained.put(IssuesEnum.valueOf(issue).toString(), IssuesEnum.valueOf(issue).getValue());
			}
		}
		this.listOfIssuesExplained = listOfIssuesExplained;
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
	
	public void setListOfIssues(List<String> list) {
		this.listOfIssues = list;
	}
	
	public Map<String, String> getListOfIssuesExplained() {
		return listOfIssuesExplained;
	}
	
	public enum IssuesEnum{
		
		MOBILE_FRIENDLY_RULE_UNSPECIFIED("Plugins incompatible with mobile devices are being used."),
		USES_INCOMPATIBLE_PLUGINS("Viewport is not specified using the meta viewport tag."),
		CONFIGURE_VIEWPORT("Viewport defined to a fixed width."),
		FIXED_WIDTH_VIEWPORT("Viewport defined to a fixed width"),
		SIZE_CONTENT_TO_VIEWPORT("Content not sized to viewport."),
		USE_LEGIBLE_FONT_SIZES("Font size is too small for easy reading on a small screen."),
		TAP_TARGETS_TOO_CLOSE("Touch elements are too close to each other.");
		
		public String issue;
		
		IssuesEnum(String value){
			issue = value;
		}
		
		public String getValue() {
			return issue;
		}
		
	}

}

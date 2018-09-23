package br.com.uhunter.performance;

import java.io.IOException;
import java.util.*;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.*;

import br.com.uhunter.performance.PerformanceMetric.PerformanceMetricValues;
import br.com.uhunter.utils.JsonValues;

public class PerformanceTest {
	
	private static final String API_URL = "https://www.googleapis.com/pagespeedonline/v4/runPagespeed";
	
	private String jsonString;
	private String loadingOverall;

	private int customization;										   

	public String doTest(String url, boolean isMobile) throws IOException {
		HttpTransport httpTransport = new NetHttpTransport();
		HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
		GenericUrl urlApi = new GenericUrl(API_URL);		
		urlApi.put("url", url);
		urlApi.put("key", JsonValues.API_KEY.getValue());
		
		if(isMobile) {
			urlApi.put("strategy", "mobile");
		}
		
		HttpRequest request = requestFactory.buildGetRequest(urlApi);
		HttpResponse httpResponse = request.execute();

		String jsonString = httpResponse.parseAsString();
		
		setJsonString(jsonString);
		
		return jsonString;
	}

	public PerformanceMetric getFCP() {
		return getMetricJsonToObject(PerformanceMetricValues.FIRST_CONTENTFUL_PAINT.getValue());
	}
	
	public PerformanceMetric getDCL() {
		return getMetricJsonToObject(PerformanceMetricValues.DOM_CONTENT_LOADED_EVENT_FIRED_MS.getValue());
	}
	
	public PerformanceMetric getMetricJsonToObject(String typeMetric) {
		
		PerformanceMetric performanceMetric = new PerformanceMetric();
		
		JsonObject jsonResult = new JsonParser().parse(jsonString).getAsJsonObject();		
		JsonObject jsonLoading = jsonResult.get(PerformanceMetricValues.LOADING_EXPERIENCE.getValue()).getAsJsonObject();
		JsonObject jsonMetrics = jsonLoading.get(PerformanceMetricValues.METRICS.getValue()).getAsJsonObject();
		JsonObject jsonFCP = jsonMetrics.get(typeMetric).getAsJsonObject();
		
		performanceMetric.setType(typeMetric);
		performanceMetric.setMedian(jsonFCP.get(PerformanceMetricValues.MEDIAN.getValue()).getAsInt());
		performanceMetric.setDistributions(setListOfDistributions(jsonFCP.get(PerformanceMetricValues.DISTRIBUTIONS.getValue()).getAsJsonArray()));
		performanceMetric.setCategory(jsonFCP.get(PerformanceMetricValues.CATEGORY.getValue()).getAsString());
		
		return performanceMetric;
		
	}

	private List<Map<String, Double>> setListOfDistributions(JsonArray jsonDistributions) {
		List<Map<String, Double>> distributions = new ArrayList<>();
		
		for(int i = 0; i < jsonDistributions.size(); i++) {
			JsonObject element = jsonDistributions.get(i).getAsJsonObject();
			
			Map<String, Double> map = new HashMap();
			
			map.put(PerformanceMetricValues.MIN.getValue(), element.get(PerformanceMetricValues.MIN.getValue()).getAsDouble());
			if(element.get(PerformanceMetricValues.MAX.getValue()) != null) {
				map.put(PerformanceMetricValues.MAX.getValue(), element.get(PerformanceMetricValues.MAX.getValue()).getAsDouble());
			}
			map.put(PerformanceMetricValues.PROPORTION.getValue(), element.get(PerformanceMetricValues.PROPORTION.getValue()).getAsDouble());
			
			distributions.add(map);
		}
		
		return distributions;
	}
	
	public String getLoadingOverall() {		
		return loadingOverall;
	}
	
	public void setLoadingOverall() {
		JsonObject jsonResult = new JsonParser().parse(jsonString).getAsJsonObject();		
		JsonObject jsonLoading = jsonResult.get(PerformanceMetricValues.LOADING_EXPERIENCE.getValue()).getAsJsonObject();
		this.loadingOverall = jsonLoading.get(PerformanceMetricValues.OVERALL_CATEGORY.getValue()).getAsString();
		
	}
	
	public int getCustomization() {
		return customization;
	}
	
	public void setCustomization() {
		JsonObject jsonResult = new JsonParser().parse(jsonString).getAsJsonObject();		
		JsonObject jsonRuleGroups = jsonResult.get(PerformanceMetricValues.RULE_GROUPS.getValue()).getAsJsonObject();
		JsonObject jsonSpeed = jsonRuleGroups.get(PerformanceMetricValues.SPEED.getValue()).getAsJsonObject();
		this.customization = jsonSpeed.get(PerformanceMetricValues.SCORE.getValue()).getAsInt();		
	}
	
	public void setJsonString(String jsonString) {
		this.jsonString = jsonString;
	}
	
	public String getJsonString() {
		return jsonString;
	}

		
}

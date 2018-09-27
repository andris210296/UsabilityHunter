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
	private PerformanceMetric fcp;
	private PerformanceMetric dcl;
	private String loadingOverall;

	private int optimizationPoints;										   

	public PerformanceTest(String url, boolean isMobile) throws IOException {
		doTest(url, isMobile);
		setFCP();
		setDCL();
		setOptimizationPoints();
		setLoadingOverall();
	}
	
	public PerformanceTest() {
		
	}

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

	public void setFCP() {
		fcp = getMetricJsonToObject(PerformanceMetricValues.FIRST_CONTENTFUL_PAINT_MS);
	}
	
	public PerformanceMetric getFCP() {
		return fcp;
	}
	
	public void setDCL() {
		dcl = getMetricJsonToObject(PerformanceMetricValues.DOM_CONTENT_LOADED_EVENT_FIRED_MS);
	}
	
	public PerformanceMetric getDCL() {
		return dcl;
	}
	
	public PerformanceMetric getMetricJsonToObject(PerformanceMetricValues typeMetric) {
		
		PerformanceMetric performanceMetric = new PerformanceMetric();
		
		JsonObject jsonResult = new JsonParser().parse(jsonString).getAsJsonObject();		
		JsonObject jsonLoading = jsonResult.get(PerformanceMetricValues.LOADING_EXPERIENCE.getValue()).getAsJsonObject();
		JsonObject jsonMetrics = jsonLoading.get(PerformanceMetricValues.METRICS.getValue()).getAsJsonObject();
		JsonObject jsonFCP = jsonMetrics.get(typeMetric.name()).getAsJsonObject();
		
		performanceMetric.setType(typeMetric);
		performanceMetric.setDescription(typeMetric.getValue());
		performanceMetric.setPageSpeedMetric(jsonFCP.get(PerformanceMetricValues.MEDIAN.getValue()).getAsInt());
		performanceMetric.setOverallMetric(jsonFCP.get(PerformanceMetricValues.CATEGORY.getValue()).getAsString());
		
		JsonArray jsonDistributions = jsonFCP.get(PerformanceMetricValues.DISTRIBUTIONS.getValue()).getAsJsonArray();		
		JsonObject elementFast = jsonDistributions.get(0).getAsJsonObject();
		performanceMetric.setFast(elementFast.get(PerformanceMetricValues.PROPORTION.getValue()).getAsDouble());		
		JsonObject elementAverage = jsonDistributions.get(1).getAsJsonObject();
		performanceMetric.setAverage(elementAverage.get(PerformanceMetricValues.PROPORTION.getValue()).getAsDouble());		
		JsonObject elementSlow = jsonDistributions.get(2).getAsJsonObject();
		performanceMetric.setSlow(elementSlow.get(PerformanceMetricValues.PROPORTION.getValue()).getAsDouble());
		
		return performanceMetric;		
	}
	
	public String getLoadingOverall() {		
		return loadingOverall;
	}
	
	public void setLoadingOverall() {
		JsonObject jsonResult = new JsonParser().parse(jsonString).getAsJsonObject();		
		JsonObject jsonLoading = jsonResult.get(PerformanceMetricValues.LOADING_EXPERIENCE.getValue()).getAsJsonObject();
		this.loadingOverall = jsonLoading.get(PerformanceMetricValues.OVERALL_CATEGORY.getValue()).getAsString();
		
	}
	
	public int getOptimizationPoints() {
		return optimizationPoints;
	}
	
	public void setOptimizationPoints() {
		JsonObject jsonResult = new JsonParser().parse(jsonString).getAsJsonObject();		
		JsonObject jsonRuleGroups = jsonResult.get(PerformanceMetricValues.RULE_GROUPS.getValue()).getAsJsonObject();
		JsonObject jsonSpeed = jsonRuleGroups.get(PerformanceMetricValues.SPEED.getValue()).getAsJsonObject();
		this.optimizationPoints = jsonSpeed.get(PerformanceMetricValues.SCORE.getValue()).getAsInt();		
	}
	
	public void setJsonString(String jsonString) {
		this.jsonString = jsonString;
	}
	
	public String getJsonString() {
		return jsonString;
	}

		
}

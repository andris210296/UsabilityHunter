package br.com.uhunter;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.*;

import org.json.JSONObject;
import org.junit.Test;

import com.google.gson.Gson;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import br.com.uhunter.performance.PerformanceMetric;
import br.com.uhunter.performance.PerformanceTest;
import br.com.uhunter.performance.PerformanceMetric.PerformanceMetricValues;

public class TestPerformance {

	private static final String URL_1 = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";

	@Test
	public void TestPerformance() throws IOException {
		
		PerformanceTest performanceTest1 = new PerformanceTest(URL_1, false);
		
		assertEquals(80, performanceTest1.getOptimizationPoints());
		assertEquals("AVERAGE", performanceTest1.getLoadingOverall());
		assertEquals(populateMetric().getType(), performanceTest1.getFCP().getType());
		assertEquals(populateMetric().getDescription(), performanceTest1.getFCP().getDescription());
		assertEquals(populateMetric().getOverallMetric(), performanceTest1.getDCL().getOverallMetric());		
	}

	@Test
	public void TestDoTest() throws IOException {
		boolean isMobile1 = false;
		PerformanceTest performanceTest = new PerformanceTest();
		String result1 = performanceTest.doTest(URL_1, isMobile1);
		assertNotNull(result1);

		boolean isMobile2 = true;
		String result2 = performanceTest.doTest(URL_1, isMobile2);
		assertNotNull(result2);
	}

	@Test
	public void TestMetric() throws IOException {

		PerformanceMetric metricExpected = populateMetric();

		PerformanceTest performanceTest = new PerformanceTest();
		performanceTest.setJsonString(createGooglePerformanceTestJson());
		performanceTest.setFCP();
		performanceTest.setDCL();
		PerformanceMetric metricFCPResult = performanceTest.getFCP();
		PerformanceMetric metricDCLResult = performanceTest.getDCL();

		assertEquals(metricExpected.getType(), metricFCPResult.getType());
		assertEquals(metricExpected.getDescription(), metricFCPResult.getDescription());
		assertEquals(metricExpected.getPageSpeedMetric(), metricFCPResult.getPageSpeedMetric());
		assertEquals(metricExpected.getFast(), metricFCPResult.getFast());
		assertEquals(metricExpected.getAverage(), metricFCPResult.getAverage());
		assertEquals(metricExpected.getSlow(), metricFCPResult.getSlow());
		assertEquals(metricExpected.getOverallMetric(), metricFCPResult.getOverallMetric());

		assertEquals(metricDCLResult.getOverallMetric(), "AVERAGE");
	}

	private PerformanceMetric populateMetric() {
		PerformanceMetric metricExpected = new PerformanceMetric();
		metricExpected.setType(PerformanceMetricValues.FIRST_CONTENTFUL_PAINT_MS);
		metricExpected.setDescription(PerformanceMetricValues.FIRST_CONTENTFUL_PAINT_MS.getValue());
		metricExpected.setPageSpeedMetric(2122);
		metricExpected.setFast(0.23512124);
		metricExpected.setAverage(0.27808622);
		metricExpected.setSlow(0.48679256);
		metricExpected.setOverallMetric("AVERAGE");
		return metricExpected;
	}

	@Test
	public void TestOveralLBetweenFCPAndDCL() {
		PerformanceTest performanceTest = new PerformanceTest();
		performanceTest.setJsonString(createGooglePerformanceTestJson());
		performanceTest.setLoadingOverall();
		String result = performanceTest.getLoadingOverall();
		assertEquals("AVERAGE", result);
	}

	@Test
	public void TestOptimizationPoints() {
		PerformanceTest performanceTest = new PerformanceTest();
		performanceTest.setJsonString(createGooglePerformanceTestJson());
		performanceTest.setOptimizationPoints();
		int result = performanceTest.getOptimizationPoints();
		assertEquals(80, result);
	}

	private String createGooglePerformanceTestJson() {

		return "{\"captchaResult\":\"CAPTCHA_NOT_NEEDED\","
				+ "\"kind\":\"pagespeedonline#result\",\"id\":\"https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal\","
				+ "\"responseCode\":200,\"title\":\"Wikipédia, a enciclopédia livre\",\"ruleGroups\":{\"SPEED\":{\"score\":80}},"
				+ "\"loadingExperience\":{\"id\":\"https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal\","
				+ "\"metrics\":{\"FIRST_CONTENTFUL_PAINT_MS\":{\"median\":2122,\"distributions\":[{\"min\":0,\"max\":1054,\"proportion\":0.23512124},"
				+ "{\"min\":1054,\"max\":2192,\"proportion\":0.27808622},{\"min\":2192,\"proportion\":0.48679256}],\"category\":\"AVERAGE\"},"
				+ "\"DOM_CONTENT_LOADED_EVENT_FIRED_MS\":{\"median\":2002,\"distributions\":[{\"min\":0,\"max\":1354,\"proportion\":0.34888732},"
				+ "{\"min\":1354,\"max\":2820,\"proportion\":0.28968453},{\"min\":2820,\"proportion\":0.36142814}],\"category\":\"AVERAGE\"}},"
				+ "\"overall_category\":\"AVERAGE\",\"initial_url\":\"https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal\"}}";
	}
}

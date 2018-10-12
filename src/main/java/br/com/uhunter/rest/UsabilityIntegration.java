package br.com.uhunter.rest;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

import org.json.JSONObject;

import br.com.uhunter.logo.LogoIdentification;
import br.com.uhunter.navigation.NavigationOnLeft;
import br.com.uhunter.performance.PerformanceTest;
import br.com.uhunter.performance.PerformanceMetric;
import br.com.uhunter.performance.PerformanceMetric.PerformanceMetricValues;
import br.com.uhunter.responsive.IsMobileFriendly;
import br.com.uhunter.utils.*;

public class UsabilityIntegration {

	private String url;
	private BufferedImage[][] webPageScreenshotMatrix;
	private ScreenshotWebPageModeler screenshotWebPageModeler;

	public UsabilityIntegration(String url) throws Exception {
		setUrl(url);

		screenshotWebPageModeler = new ScreenshotWebPageModeler(url);

		setWebPageScreenshotMatrix(screenshotWebPageModeler.getImagePieces());
	}

	public Map<String, String> doLogoTest() {
		Map<String, String> map = new HashMap<>();

		try {

			LogoIdentification logoIdentification = new LogoIdentification();
			InputStream inputStream = screenshotWebPageModeler.getImagePiece(0, 0);
			String logo = logoIdentification.isThereALogoOnTopLeftCorner(getUrl(), inputStream);

			if (logo != null) {
				map.put(JsonValues.LOGO_NAME.getValue(), logo);
				map.put(JsonValues.RESULT.getValue(), String.valueOf(true));
			} else {
				throw new Exception();
			}

		} catch (Exception e) {
			map.put(JsonValues.LOGO_NAME.getValue(), e.getMessage());
			map.put(JsonValues.RESULT.getValue(), String.valueOf(false));

		}

		return map;
	}

	public Map<String, String> doNavigationOnLeftCornerTest() {
		Map<String, String> map = new HashMap<>();

		try {
			
			NavigationOnLeft navigationOnLeft = new NavigationOnLeft(screenshotWebPageModeler.getByteImagePiece(0, 0), url);
			boolean responseNavigationOnTheLeftSide = navigationOnLeft.isTheNavigationOnTheLeftSide();

			if (responseNavigationOnTheLeftSide) {
				map.put(JsonValues.NAVIGATION_ON_LEFT_CORNER.getValue(), String.valueOf(true));

				if (navigationOnLeft.getQuantityOfItemsOnMiddleLeft() >= 5) {
					if (navigationOnLeft.areThereEnoughItemsOnAlphabeticalOrder()) {
						map.put(JsonValues.HICKSLAW.getValue(), String.valueOf(true));
					} else {
						map.put(JsonValues.HICKSLAW.getValue(), String.valueOf(false));
					}
				} else {
					map.put(JsonValues.HICKSLAW.getValue(), JsonValues.NOT_APPLICABLE.getValue());
				}
			} else {

				map.put(JsonValues.NAVIGATION_ON_LEFT_CORNER.getValue(), String.valueOf(false));
				map.put(JsonValues.HICKSLAW.getValue(), JsonValues.NOT_APPLICABLE.getValue());
			}

		} catch (Exception e) {
			map.put(JsonValues.NAVIGATION_ON_LEFT_CORNER.getValue(), e.getMessage());
		}
		return map;
	}

	public List<Map> doIsMobileFriendlyTest() {

		List<Map> map = new ArrayList<>();
		Map<String, String> mapValue = new HashMap<>();
		Map<String, Map<String, String>> mapIssues = new HashMap<>();

		try {
			IsMobileFriendly isMobileFriendly = new IsMobileFriendly(getUrl());
			if (isMobileFriendly.getResult()) {

				mapValue.put(JsonValues.IS_MOBILE_FRIENDLY.getValue(), String.valueOf(true));
				map.add(mapValue);

			} else {

				mapValue.put(JsonValues.IS_MOBILE_FRIENDLY.getValue(), String.valueOf(false));
				mapIssues.put(JsonValues.LIST_OF_ISSUES.getValue(), isMobileFriendly.getListOfIssuesExplained());
				map.add(mapValue);
				map.add(mapIssues);
			}

		} catch (Exception e) {
			mapValue.put(JsonValues.IS_MOBILE_FRIENDLY.getValue(), e.getStackTrace().toString());
			map.add(mapValue);
		}

		return map;
	}

	public Map<String, Object> doPerformanceTest() {

		Map<String, Object> performanceMap = new HashMap<>();

		try {

			PerformanceTest performanceTestDesktop = new PerformanceTest(getUrl(), false);
			PerformanceTest performanceTestMobile = new PerformanceTest(getUrl(), true);
			
			
			Map<String, Map<String, Object>> mapDevices = new HashMap<>();
			mapDevices.put(JsonValues.MOBILE_TEST.getValue(), listDevice(performanceTestMobile));
			mapDevices.put(JsonValues.DESKTOP_TEST.getValue(), listDevice(performanceTestDesktop));


			performanceMap.put(JsonValues.PERFORMANCE_TEST.getValue(), mapDevices);
			
			
			
			
			

		} catch (Exception e) {
			performanceMap.put(JsonValues.PERFORMANCE_TEST.getValue(), e.getStackTrace().toString());
		}

		return performanceMap;
	}

	private Map<String, Object> listDevice(PerformanceTest performanceTest) {

		Map<String, Object> deviceMap = new HashMap<>();
		deviceMap.put(JsonValues.OPTIMIZATION_POINTS.getValue(), performanceTest.getOptimizationPoints());
		deviceMap.put(JsonValues.LOADING_OVERALL.getValue(), performanceTest.getLoadingOverall());
		deviceMap.put(JsonValues.LOADING_PERFORMANCE.getValue(), listLoadingPerformance(performanceTest));

		return deviceMap;
	}

	private Map<String, List<Map>> listLoadingPerformance(PerformanceTest performanceTest) {

		PerformanceMetric fcp = performanceTest.getFCP();
		PerformanceMetric dcl = performanceTest.getDCL();
		
		Map<String, List<Map>> mapMetrics = new HashMap<>();
		List<Map> metrics = new ArrayList<>();
		metrics.add(getMetric(fcp.getType(), fcp.getDescription(),fcp.getOverallMetric(), fcp.getPageSpeedMetric(),
				fcp.getFast(), fcp.getAverage(), fcp.getSlow()));
		metrics.add(getMetric(dcl.getType(), dcl.getDescription(),dcl.getOverallMetric(), dcl.getPageSpeedMetric(),
				dcl.getFast(), dcl.getAverage(), dcl.getSlow()));

		mapMetrics.put(JsonValues.METRICS.getValue(), metrics);

		return mapMetrics;
	}

	private Map<String, Object> getMetric(PerformanceMetricValues type, String typeDescription,
			String pageContentLoadDistributionAverage, Integer pageSpeed, Double fast, Double average, Double slow) {

		Map<String, Object> mapMetric = new HashMap<>();
		mapMetric.put(JsonValues.TYPE.getValue(), type);
		mapMetric.put(JsonValues.DESCRIPTION.getValue(), typeDescription);
		mapMetric.put(JsonValues.PAGE_CONTENT_LOAD_DISTRIBUTION_AVERAGE.getValue(), pageContentLoadDistributionAverage);
		mapMetric.put(JsonValues.PAGE_SPEED.getValue(), pageSpeed);
		mapMetric.put(JsonValues.PAGE_CONTENT_LOAD_DISTRIBUTION.getValue(),
				getPageContentLoadDistribution(fast, average, slow));

		return mapMetric;
	}

	private Map<String, Double> getPageContentLoadDistribution(Double fast, Double average, Double slow) {
		Map<String, Double> pageContentLoadDistributionMap = new HashMap<>();
		pageContentLoadDistributionMap.put(JsonValues.FAST.getValue(), fast);
		pageContentLoadDistributionMap.put(JsonValues.AVERAGE.getValue(), average);
		pageContentLoadDistributionMap.put(JsonValues.SLOW.getValue(), slow);

		return pageContentLoadDistributionMap;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public BufferedImage[][] getWebPageScreenshotMatrix() {
		return webPageScreenshotMatrix;
	}

	public void setWebPageScreenshotMatrix(BufferedImage[][] webPageScreenshotMatrix) {
		this.webPageScreenshotMatrix = webPageScreenshotMatrix;
	}

}

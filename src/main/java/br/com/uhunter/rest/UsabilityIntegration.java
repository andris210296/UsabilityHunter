package br.com.uhunter.rest;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import br.com.uhunter.logo.LogoIdentification;
import br.com.uhunter.navigation.NavigationOnLeft;
import br.com.uhunter.performance.PerformanceTest;
import br.com.uhunter.performance.PerformanceMetric;
import br.com.uhunter.performance.PerformanceMetric.PerformanceMetricValues;
import br.com.uhunter.responsive.IsMobileFriendly;
import br.com.uhunter.utils.*;

public class UsabilityIntegration {

	private String url;
	private byte[][][] webPageScreenshotMatrix;
	private ScreenshotWebPageModeler screenshotWebPageModeler;

	public UsabilityIntegration(String url) throws Exception {
		setUrl(url);

		screenshotWebPageModeler = new ScreenshotWebPageModeler(url);

		setWebPageScreenshotMatrix(screenshotWebPageModeler.getByteImageMatrix());
	}

	public Map<String, Object> doLogoTest() {
		Map<String, Object> map = new HashMap<>();

		try {

			LogoIdentification logoIdentification = new LogoIdentification();
			byte[] byteImage = screenshotWebPageModeler.getByteImagePiece(0, 0);
			String logo = logoIdentification.isThereALogoOnTopLeftCorner(getUrl(), byteImage);

			if (logo == null) {

				ScreenshotWebPageModeler screenshotWebPageModeler = new ScreenshotWebPageModeler();
				screenshotWebPageModeler.setByteImage(byteImage);
				screenshotWebPageModeler.setHorizontalPieces(2);
				screenshotWebPageModeler.setVerticalPieces(2);
				byte[][][] matrix = screenshotWebPageModeler.getBytesImagePieces();

				for (int i = 0; i <= 1; i++) {
					if (logo != null) {
						break;
					}
					for (int j = 0; j <= 1; j++) {
						logo = logoIdentification.isThereALogoOnTopLeftCorner(getUrl(), matrix[i][j]);
						if (logo != null) {
							break;
						}
					}
				}

				if (logo != null) {
					map.put(JsonValues.LOGO_NAME.getValue(), logo);
					map.put(JsonValues.RESULT.getValue(), true);
				} else {
					map.put(JsonValues.RESULT.getValue(), false);
				}

			} else {
				map.put(JsonValues.LOGO_NAME.getValue(), logo);
				map.put(JsonValues.RESULT.getValue(), true);
			}

		} catch (Exception e) {
			map.put(JsonValues.LOGO_NAME.getValue(), e.getMessage());
			map.put(JsonValues.RESULT.getValue(), false);

		}

		return map;
	}

	public Map<String, Object> doNavigationOnLeftCornerTest() {
		Map<String, Object> map = new HashMap<>();

		try {

			NavigationOnLeft navigationOnLeft = new NavigationOnLeft(screenshotWebPageModeler.getByteImagePiece(0, 0),
					url);
			boolean responseNavigationOnTheLeftSide = navigationOnLeft.isTheNavigationOnTheLeftSide();

			if (responseNavigationOnTheLeftSide) {
				map.put(JsonValues.NAVIGATION_ON_LEFT_CORNER.getValue(), true);

				if (navigationOnLeft.getQuantityOfItemsOnMiddleLeft() >= 5) {
					if (navigationOnLeft.areThereEnoughItemsOnAlphabeticalOrder()) {
						map.put(JsonValues.HICKSLAW.getValue(), true);
					} else {
						map.put(JsonValues.HICKSLAW.getValue(), false);
					}
				} else {
					map.put(JsonValues.HICKSLAW.getValue(), JsonValues.NOT_APPLICABLE.getValue());
				}
			} else {

				map.put(JsonValues.NAVIGATION_ON_LEFT_CORNER.getValue(), false);
				map.put(JsonValues.HICKSLAW.getValue(), JsonValues.NOT_APPLICABLE.getValue());
			}

		} catch (Exception e) {
			map.put(JsonValues.NAVIGATION_ON_LEFT_CORNER.getValue(), e.getMessage());
		}
		return map;
	}

	public Map<String, Object> doIsMobileFriendlyTest() {
		Map<String, Object> map = new HashMap<>();

		try {
			IsMobileFriendly isMobileFriendly = new IsMobileFriendly(getUrl());
			if (isMobileFriendly.getResult()) {
				map.put(JsonValues.IS_MOBILE_FRIENDLY.getValue(), true);

			} else {
				map.put(JsonValues.IS_MOBILE_FRIENDLY.getValue(), false);
				map.put(JsonValues.LIST_OF_ISSUES.getValue(), isMobileFriendly.getListOfIssuesExplained());
			}

		} catch (Exception e) {
			map.put(JsonValues.IS_MOBILE_FRIENDLY.getValue(),e.getMessage());
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
		metrics.add(getMetric(fcp.getType(), fcp.getDescription(), fcp.getOverallMetric(), fcp.getPageSpeedMetric(),
				fcp.getFast(), fcp.getAverage(), fcp.getSlow()));
		metrics.add(getMetric(dcl.getType(), dcl.getDescription(), dcl.getOverallMetric(), dcl.getPageSpeedMetric(),
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

	public byte[][][] getWebPageScreenshotMatrix() {
		return webPageScreenshotMatrix;
	}

	public void setWebPageScreenshotMatrix(byte[][][] webPageScreenshotMatrix) {
		this.webPageScreenshotMatrix = webPageScreenshotMatrix;
	}

}

package br.com.uhunter;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

import org.json.JSONObject;

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

			ImageUtils.inputStreamToFile(screenshotWebPageModeler.getImagePiece(0, 0));

			File file = new File("imgTest/image.jpg");
			BufferedImage bufferedImage = ImageIO.read(file);

			InputStream inputStream = new FileInputStream(file);

			NavigationOnLeft navigationOnLeft = new NavigationOnLeft(inputStream, bufferedImage, url);
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
			map.put(JsonValues.NAVIGATION_ON_LEFT_CORNER.getValue(), e.getStackTrace().toString());
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

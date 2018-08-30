package br.com.uhunter;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

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

	public JSONObject doLogoTest() {

		JSONObject json = new JSONObject();

		try {

			LogoIdentification logoIdentification = new LogoIdentification();
			InputStream inputStream = screenshotWebPageModeler.getImagePiece(0,0);
			String logo = logoIdentification.isThereALogoOnTopLeftCorner(getUrl(), inputStream);

			if (logo != null) {
				json.put("logoName", logo);
				json.put("result", true);
			} else {
				throw new Exception();
			}

		} catch (Exception e) {
			json.put("logoName", e.getMessage());
			json.put("result", false);

		}

		return json;
	}
	
	public JSONObject doNavigationOnLeftCornerTest() {
		JSONObject json = new JSONObject();
		
		try {
						
			ImageUtils.inputStreamToFile(screenshotWebPageModeler.getImagePiece(0,0));
			
			File file = new File("imgTest/image.jpg");
			BufferedImage bufferedImage = ImageIO.read(file);
			
			InputStream inputStream = new FileInputStream(file);
						
			NavigationOnLeft navigationOnLeft = new NavigationOnLeft(inputStream, bufferedImage, url);
			boolean responseNavigationOnTheLeftSide = navigationOnLeft.isTheNavigationOnTheLeftSide();
			
			if(responseNavigationOnTheLeftSide) {
				json.put("navigationOnLeftCorner", true);
				
				if(navigationOnLeft.getQuantityOfItemsOnMiddleLeft() >= 5) {
					if(navigationOnLeft.areThereEnoughItemsOnAlphabeticalOrder()) {						
						json.put("hicksLaw", true);	
					}
					else {
						json.put("hicksLaw", false);							
					}
				}
				else {
					json.put("hicksLaw", "not applicable");					
				}
			}else {
				
				json.put("navigationOnLeftCorner", false);
				json.put("hicksLaw", "not applicable");
			}
			
			
		}catch (Exception e) {
			json.put("navigationOnLeftCorner", e.getStackTrace());
		}
		return json;
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

package br.com.uhunter;

import java.awt.image.BufferedImage;
import java.io.InputStream;

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

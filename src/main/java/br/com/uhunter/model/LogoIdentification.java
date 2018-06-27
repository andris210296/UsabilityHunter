package br.com.uhunter.model;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class LogoIdentification {

	private static int TIMEOUT = 10000;

	private Elements elementsTitle;

	private String url;

	public LogoIdentification(String url) throws IOException {
		setUrl(url);
		Document document = Jsoup.connect(getUrl()).timeout(TIMEOUT).get();
		setElementsTitle(document.select("title"));
	}

	public String returnLogo() throws Exception {

		ScreenshotWebPageModeler screenshotModeler = new ScreenshotWebPageModeler(getUrl());

		String logoResult1 = detectLogoByImage(screenshotModeler);

		// String logoResult =
		// GoogleVision.detectText(screenshotModeler.getImagePiece(0, 0));

		return logoResult1;

	}

	public String detectLogoByImage(ScreenshotWebPageModeler screenshotModeler) throws Exception {
		List<String> logoResult = GoogleVision.detectLogos(screenshotModeler.getImagePiece(0, 0));

		for (String logo : logoResult) {
			String logoReplaced = replaceAccentuation(logo);
			String titleReplaced = replaceAccentuation(getElementsTitle().text());
			if (titleReplaced.toLowerCase().contains(logoReplaced.toLowerCase()) || logoResult.size() == 1) {

				return logo;
			}
		}
		return null;

	}

	private String replaceAccentuation(String string) {
		return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	}

	private Elements getElementsTitle() {
		return elementsTitle;
	}

	private void setElementsTitle(Elements elementsTitle) {
		this.elementsTitle = elementsTitle;
	}

	private String getUrl() {
		return url;
	}

	private void setUrl(String url) {
		this.url = url;
	}

}

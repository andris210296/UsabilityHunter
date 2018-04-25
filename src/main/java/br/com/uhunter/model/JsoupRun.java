package br.com.uhunter.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.text.Collator;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hsqldb.lib.HashMap;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionScopes;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.Image;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

public class JsoupRun {

	private static int TIMEOUT = 10000;

	private static final String APPLICATION_NAME = "Google-VisionTextSample/1.0";
	private static final int MAX_RESULTS = 6;
	private static final int BATCH_SIZE = 10;

	/**
	 * This method returns the html Document of the given url.
	 * 
	 * @param url
	 * @return Document
	 * @throws IOException
	 */
	private static Document urlToDocument(String url) throws IOException {
		Document document = Jsoup.connect(url).timeout(TIMEOUT).get();
		return document;
	}

	/**
	 * This method gets all values and its hrefs from the page. Example: When a href
	 * attribute is found, this method returns a LinkedHashMap with "Play" and
	 * "href="https://play.google.com/?hl=pt-BR&amp;tab=w8" from the tag below:
	 * <a class="gb1" href="https://play.google.com/?hl=pt-BR&amp;tab=w8">Play</a>
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static LinkedHashMap<String, String> pageHrefValue(String url) throws IOException {

		Elements links = urlToDocument(url).select("a[href]");

		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();

		for (Element link : links) {
			map.put(link.text(), link.attr("abs:href"));
		}

		return map;
	}

	/**
	 * 
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static int howManyWords(String url) throws IOException {

		Elements text = urlToDocument(url).select("a[href]");

		return 0;

	}

	/**
	 * This method returns all images that are in the web page.
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<String> getImagesPage(String url) throws IOException {

		Elements media = urlToDocument(url).select("[src]");

		ArrayList<String> images = new ArrayList<>();

		for (Element src : media) {
			if (src.tagName().equals("img")) {
				images.add(src.attr("abs:src"));
			}
		}

		return images;
	}

	public static LinkedHashMap<String, LinkedHashMap<String, String>> getLogoPage(String url) throws Exception {

		List<String> logoResult = new ArrayList<>();
		
		ScreenshotWebPageModeler screenshotModeler = new ScreenshotWebPageModeler(url);

		LinkedHashMap<String, LinkedHashMap<String, String>> map = new LinkedHashMap<>();
		LinkedHashMap<String, String> mapDescription = new LinkedHashMap<>();

		Elements elements = urlToDocument(url).select("title");	
						
		try {
			logoResult = GoogleVision.detectLogos(screenshotModeler.getImagePiece(0, 0));
		}catch (Exception e) {
			logoResult = GoogleVision.detectText(screenshotModeler.getImagePiece(0, 0));
		}

		for (String logo : logoResult) {
			String logoReplaced = Normalizer.normalize(logo, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
			String titleReplaced = Normalizer.normalize(elements.text(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
			if (titleReplaced.toLowerCase().contains(logoReplaced.toLowerCase()) || logoResult.size() == 1) {
				mapDescription.put("result", "true");
				mapDescription.put("logoName", logo);
			}
		}		
		if (mapDescription.size() == 0) {
			mapDescription.put("result", "false");
			mapDescription.put("logoName", elements.text().toLowerCase());
		}
		map.put("Logo on top-left", mapDescription);

		return map;

	}
}

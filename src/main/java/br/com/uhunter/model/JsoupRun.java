package br.com.uhunter.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.*;

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
import com.google.api.services.vision.v1.model.*;
import com.google.appengine.api.ThreadManager;
import com.google.common.collect.ImmutableList;

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

	public static LinkedHashMap<String, String> getLogoPage(String url) throws Exception {

		ArrayList<String> images = getImagesPage(url);

		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();

		for (String string : images) {

			Thread thread = ThreadManager.createBackgroundThread(new Runnable() {
				@Override
				public void run() {
					try {
						map.put(string, GoogleVision.detectLogosGcs(string));
						System.out.println("running from thread!");

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			});

			thread.start();

		}
		return map;

	}
}

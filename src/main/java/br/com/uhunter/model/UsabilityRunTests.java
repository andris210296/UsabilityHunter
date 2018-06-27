package br.com.uhunter.model;


import java.io.IOException;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;


public class UsabilityRunTests {

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

		LinkedHashMap<String, LinkedHashMap<String, String>> map = new LinkedHashMap<>();
		LinkedHashMap<String, String> mapDescription = new LinkedHashMap<>();

		
		LogoIdentification logoIdentification = new LogoIdentification(url);
		String logo = logoIdentification.returnLogo();
		
		if (logo != null) {
			mapDescription.put("result", "true");
			mapDescription.put("logoName", logo);
		}
				
		if (mapDescription.size() == 0) {
			mapDescription.put("result", "false");
			mapDescription.put("logoName", "não foi");
		}
		map.put("Logo on top-left", mapDescription);

		return map;

	}
}

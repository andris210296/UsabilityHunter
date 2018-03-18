package br.com.uhunter.model;

import java.io.IOException;
import java.util.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class JsoupRun {
	
	private static int TIMEOUT = 10000;
	
	
	/**
	 * This method returns the html Document of the given url.
	 * 
	 * @param url
	 * @return Document
	 * @throws IOException
	 */
	private static Document urlToDocument(String url) throws IOException{
		Document document = Jsoup.connect(url).timeout(TIMEOUT).get();
		return document;
	}
	
	/**
	 * This method gets all values and its hrefs from the page.
	 * Example: When a href attribute is found, this method returns
	 * a LinkedHashMap with "Play" and "href="https://play.google.com/?hl=pt-BR&amp;tab=w8" from the tag below:
	 * 	<a class="gb1" href="https://play.google.com/?hl=pt-BR&amp;tab=w8">Play</a> 
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static LinkedHashMap pageHrefValue(String url) throws IOException{
				
		Elements links = urlToDocument(url).select("a[href]");
		
		LinkedHashMap<String,String> map = new LinkedHashMap<String, String>();
		
		for (Element link : links) {
            map.put(link.text(),link.attr("abs:href"));
        }				
		
		return map;		
	}
	
	public static int howManyWords(String url) throws IOException{
				
		//Gets the html document of the given url.
		Document document = Jsoup.connect(url).timeout(TIMEOUT).get();
		
		
		
		return 0;
		
	}

	private static String print(String msg, Object... args) {

        return String.format(msg, args);

    }
	
	private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width-1) + ".";
        else
            return s;
    }
	

}

package br.com.uhunter;

import java.io.IOException;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class JsoupRun {
	
	private static int TIMEOUT = 6000;
	
	
	public static String pageWords(String url) throws IOException{
		
		//Gets the html document of the given url.
		Document document = Jsoup.connect(url).timeout(TIMEOUT).get();
		
		Elements elements = document.select("!DOCTYPE html");
		
		String words = elements.text();
		
		return words;
		
	}
	
	public static int howManyWords(String url) throws IOException{
				
		//Gets the html document of the given url.
		Document document = Jsoup.connect(url).timeout(TIMEOUT).get();
		
		
		
		return 0;
		
	}
	

}

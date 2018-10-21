package br.com.uhunter.logo;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import br.com.uhunter.utils.GoogleVision;

public class LogoIdentification {

	private String pageTitle;
	
	
	public String isThereALogoOnTopLeftCorner(String url, byte[] byteImage) throws Exception {
		
		List<String> logos = GoogleVision.detectLogo(byteImage);
		setPageTitle(getWebPageTitleFromHtml(url));
		
		return isStringInsideOfTheTitle(logos);
	}

	public String isStringInsideOfTheTitle(List<String> strings) {

		for (String string : strings) {
			for (String stringCombination : wordsCombination(string)) {
				for (String titleCombination : wordsCombination(getPageTitle())) {
					if (areTheseStringSimilar(replaceAccentuation(titleCombination), replaceAccentuation(stringCombination))) {
						return stringCombination;
					}
				}
			}
		}
		return null;
	}
	
	public boolean areTheseStringSimilar(String string1, String string2) {

		LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
		int result = levenshteinDistance.apply(string1, string2);

		if (result <= 3) {
			return true;
		}

		return false;
	}

	public List<String> wordsCombination(String string) {
        
		String[] words = string.split(" ");
		List<String> combinations = new ArrayList<>();	
				
		for (int i = 0; i < words.length; i++) {
			combinations.add(words[i]);
			String b = words[i];
			for(int j = i; j < words.length - 1; j++) {
				
				b+=words[j+1];
				combinations.add(b);
			}
		}
						
		return combinations;
	}
	
	public static String getWebPageTitleFromHtml(String url) throws Exception {
		Document document = Jsoup.connect(url).timeout(10000).get();
		return document.select("title").text();
	}
	
	private String replaceAccentuation(String string) {
		return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	}

	public void setPageTitle(String title) {
		pageTitle = title;
	}

	public String getPageTitle() {
		return pageTitle;
	}
	
	public String getPageTitleWordsTogether() {
		return pageTitle.replace(" ","");
	}
	
}

package br.com.uhunter;

import java.awt.image.BufferedImage;
import java.io.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import java.util.*;

import com.google.cloud.vision.v1.Vertex;

public class NavigationOnLeft {

	public static List<ParagraphText> getParagraphs(String url) throws Exception {
		
		List<ParagraphText> paragraphs = new ArrayList<>();
        
		List<List<Vertex>> vertexes = GoogleVision.detectBlockVertexesFromImage(new FileInputStream(url));
		
		BufferedImage bfImage = ImageUtils.inputStreamToBufferedImage(new FileInputStream(url));
		
		for (List<Vertex> vertex : vertexes) {	
			
			InputStream is = ImageUtils.getPiece(bfImage, vertex);
			
			List<String> lines = GoogleVision.detectText(is);						
			paragraphs.add(new ParagraphText(vertex, lines));
		}				
		return paragraphs;
	}

	public static LinkedHashMap<String, String> getLinksFromPage(String url) throws IOException {
		Document document = Jsoup.connect(url).timeout(10000).get();
		
		Elements links = document.select("a[href]");

		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();

		for (Element link : links) {
			map.put(link.text(), link.attr("abs:href"));
		}

		return map;
	}

	public static boolean isTheNavigationOnTheLeftSide(String url, List<ParagraphText> paragraphs) throws Exception {
		
		LinkedHashMap<String, String> links = getLinksFromPage(url);
		
		int count = 0;
		
		for (ParagraphText paragraphText : paragraphs) {
			for (String link : links.keySet()) {
				if(paragraphText.getLines().contains(link))
				{
					count+=1;
				}
			}			
		}
				
		return isTheQuantityOfLinksByWordsEnough(paragraphs, count);
	}

	public static boolean isTheQuantityOfLinksByWordsEnough(List<ParagraphText> paragraphs, int qtdOfLinks) {
		
		int qtdLines = 0;
		double percentage = 0.0;
		
		for (ParagraphText paragraph : paragraphs) {
			
			qtdLines = paragraph.getLines().size();
			percentage= ((double)qtdOfLinks / qtdLines) * 100;
			
			if(percentage >= 30) {
				return true;
			}
		}
		return false;
	}

}

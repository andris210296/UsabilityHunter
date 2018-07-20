package br.com.uhunter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import org.junit.Test;

import com.google.cloud.vision.v1.Vertex;

public class NavigationOnLeftTest {
	
	@Test
	public void getVertexesFromImage() throws Exception {
				
		List<Vertex> vertexes = populateListVertex1();	
	
		List<List<Vertex>> resultsText1 = getResultsFromGoogleVisionBlockVertexesFromImage("imgTest/wikipediaNavigation.jpg");	
		
		assertEquals(resultsText1.get(2), vertexes);

	}
	

	private List<List<Vertex>> getResultsFromGoogleVisionBlockVertexesFromImage(String fileUrl) throws Exception {
		File file = new File(fileUrl);
		InputStream inputStream = new FileInputStream(file);
		return GoogleVision.detectBlockVertexesFromImage(inputStream);
	}
	
	@Test
	public void getCorrectTextFromGoogleVisionTest() throws Exception {
		
		List<String> lines1 = populateLinesOfAParagraph1();
		
		List<Vertex> vertexes = populateListVertex1();
		
		BufferedImage bfImage = ImageUtils.inputStreamToBufferedImage( new FileInputStream("imgTest/wikipediaNavigation.jpg"));
		InputStream inputStream = ImageUtils.getPiece(bfImage, vertexes);
		
		List<String> result1 = GoogleVision.detectText(inputStream);
		
		assertEquals(result1, lines1);
		
	}

	@Test
	public void listOfParagraphFromPage() throws Exception {
		
		List<String> lines1 = populateLinesOfAParagraph1();		
		List<Vertex> vertexes = populateListVertex1();
		
		ParagraphText paragraph = new ParagraphText(vertexes, lines1);
		
		List<ParagraphText> paragraphs = NavigationOnLeft.getParagraphs("imgTest/wikipediaNavigation.jpg");
		
		assertEquals(paragraphs.get(2).getLines(), paragraph.getLines());
		assertEquals(paragraphs.get(2).getVertexes(), paragraph.getVertexes());
		
		
		List<String> lines2 = populateLinesOfAParagraph2();		
		List<Vertex> vertexes2 = populateListVertex2();
		
		ParagraphText paragraph2 = new ParagraphText(vertexes2, lines2);
		
		List<ParagraphText> paragraphs2 = NavigationOnLeft.getParagraphs("imgTest/uspNavigation.jpg");
		
		assertEquals(paragraphs2.get(6).getLines(), paragraph2.getLines());
		assertEquals(paragraphs2.get(6).getVertexes(), paragraph2.getVertexes());
	}
	
	@Test
	public void blockHasLinkTest() throws Exception {

		List<String> lines1 = populateLinesOfAParagraph1();		
		String url1 = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";		
		LinkedHashMap<String, String> links1 = NavigationOnLeft.getLinksFromPage(url1);
		
		for (String lines : lines1) {
			assertTrue(links1.keySet().contains(lines));
		}
		
		
		String url2 = "https://cursosextensao.usp.br/course/view.php?id=131&section=0";		
		LinkedHashMap<String, String> links2 = NavigationOnLeft.getLinksFromPage(url2);
		
		List<String> lines2 = new ArrayList<>();
		lines2.add("Página inicial");
		lines2.add("Lezione 1");
		lines2.add("Lezione 7");
		lines2.add("Lezione 16");
		
		for (String lines : lines2) {
			assertTrue(links2.keySet().contains(lines));
		}	
						
	}
	
	@Test
	public void isTheQuantityOfLinksByWordsEnoughInAPieceOfWebPage() {
		
		List<ParagraphText> paragraphs = new ArrayList<>();
		List<String> blockLines = populateLinesOfAParagraph1();
		paragraphs.add(new ParagraphText(populateListVertex1(), blockLines));
		
		int qtdOfLinks1 = 3;
		
		boolean result1 = NavigationOnLeft.isTheQuantityOfLinksByWordsEnough(paragraphs, qtdOfLinks1);
		
		assertTrue(result1);
	}
	
	
	@Test
	public void areThereLinksOnThePage() throws Exception{
	
		List<ParagraphText> paragraphs1 = NavigationOnLeft.getParagraphs("imgTest/wikipediaNavigation.jpg");
		String url1 = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";	
		
		boolean response1 = NavigationOnLeft.isTheNavigationOnTheLeftSide(url1, paragraphs1);
		
		assertTrue(response1);
		
		List<ParagraphText> paragraphs2 = NavigationOnLeft.getParagraphs("imgTest/uspNavigation.jpg");
		String url2 = "https://cursosextensao.usp.br/course/view.php?id=131&section=0";	
		
		boolean response2 = NavigationOnLeft.isTheNavigationOnTheLeftSide(url2, paragraphs2);
		
		assertTrue(response2);
	}
					

	private List<String> populateLinesOfAParagraph1() {
		List<String> lines1 = new ArrayList<>();
		lines1.add("Página principal");
		lines1.add("Conteúdo destacado");
		lines1.add("Eventos atuais");
		lines1.add("Esplanada");
		lines1.add("Página aleatória");
		lines1.add("Portais");
		lines1.add("Informar um erro");
		lines1.add("Loja da Wikipédia");
		return lines1;
	}
	
	private List<String> populateLinesOfAParagraph2() {
		List<String> lines2 = new ArrayList<>();
		lines2.add("Páginas do site");
		lines2.add("Curso atual");
		return lines2;
	}
	
	private List<Vertex> populateListVertex1() {
		List<Vertex> vertexes = new ArrayList<>();
		vertexes.add(Vertex.newBuilder().setX(26).setY(196).build());
		vertexes.add(Vertex.newBuilder().setX(135).setY(196).build());
		vertexes.add(Vertex.newBuilder().setX(135).setY(345).build());
		vertexes.add(Vertex.newBuilder().setX(26).setY(345).build());
		return vertexes;
	}
	
	private List<Vertex> populateListVertex2() {
		List<Vertex> vertexes = new ArrayList<>();
		vertexes.add(Vertex.newBuilder().setX(43).setY(231).build());
		vertexes.add(Vertex.newBuilder().setX(138).setY(233).build());
		vertexes.add(Vertex.newBuilder().setX(137).setY(273).build());
		vertexes.add(Vertex.newBuilder().setX(42).setY(271).build());
		return vertexes;
	}
	
	
}

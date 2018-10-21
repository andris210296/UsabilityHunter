package br.com.uhunter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

import javax.ejb.Init;
import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.openqa.selenium.WebDriver.Navigation;

import com.google.cloud.vision.v1.Vertex;

import br.com.uhunter.navigation.NavigationOnLeft;
import br.com.uhunter.navigation.ParagraphText;
import br.com.uhunter.utils.GoogleVision;
import br.com.uhunter.utils.ImageUtils;

public class TestNavigationOnLeft {

	private static final String URL_1 = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";
	private static final File FILE_1 = new File("imgTest/wikipediaMatrix_0_0.jpg");
	private NavigationOnLeft navigationOnLeft1;

	private static final String URL_2 = "https://cursosextensao.usp.br/course/view.php?id=131&section=0";
	private static final File FILE_2 = new File("imgTest/uspPage.jpg");
	private NavigationOnLeft navigationOnLeft2;
	
	private static final String URL_3 = "https://pt.stackoverflow.com/";
	private static final File FILE_3 = new File("imgTest/stackoverflow.jpg");
	private NavigationOnLeft navigationOnLeft3;

	@Before
	public void initialConfig() throws Exception {
		byte[] byteImage1 = Files.readAllBytes(FILE_1.toPath());
		navigationOnLeft1 = new NavigationOnLeft(byteImage1, URL_1);
		
		byte[] byteImage2 = Files.readAllBytes(FILE_2.toPath());
		navigationOnLeft2 = new NavigationOnLeft(byteImage2, URL_2);
				
		byte[] byteImage3 = Files.readAllBytes(FILE_3.toPath());
		navigationOnLeft3 = new NavigationOnLeft(byteImage3, URL_3);
	}

	@Test
	public void TestGetVertexesFromImage() throws Exception {

		List<Vertex> vertexes = populateListVertex1();
		List<List<Vertex>> resultsText1 = getResultsFromGoogleVisionBlockVertexesFromImage(FILE_1);
		assertEquals(resultsText1.get(2), vertexes);

	}

	private List<List<Vertex>> getResultsFromGoogleVisionBlockVertexesFromImage(File file) throws Exception {
		byte[] byteImage = Files.readAllBytes(file.toPath());
		return GoogleVision.detectBlockVertexesFromImage(byteImage);
	}

	@Test
	public void TestGetCorrectTextFromGoogleVisionTest() throws Exception {

		List<String> lines1 = populateLinesOfAParagraph1();
		List<Vertex> vertexes = populateListVertex1();

		BufferedImage bfImage = ImageUtils.inputStreamToBufferedImage(new FileInputStream(FILE_1));
		byte[] byteImage = ImageUtils.getPiece(bfImage, vertexes);

		List<String> result1 = GoogleVision.detectText(byteImage);
		assertEquals(result1, lines1);
	}

	@Test
	public void TestListOfParagraphFromPage() throws Exception {

		List<String> lines1 = populateLinesOfAParagraph1();
		List<Vertex> vertexes = populateListVertex1();

		ParagraphText paragraph = new ParagraphText(vertexes, lines1);

		List<ParagraphText> paragraphs = navigationOnLeft1.getParagraphs();

		assertEquals(paragraphs.get(2).getLines(), paragraph.getLines());
		assertEquals(paragraphs.get(2).getVertexes(), paragraph.getVertexes());

		
		List<String> lines2 = populateLinesOfAParagraph2();
		List<Vertex> vertexes2 = populateListVertex2();

		ParagraphText paragraph2 = new ParagraphText(vertexes2, lines2);

		List<ParagraphText> paragraphs2 = navigationOnLeft2.getParagraphs();

		assertEquals(paragraphs2.get(6).getLines(), paragraph2.getLines());
		assertEquals(paragraphs2.get(6).getVertexes(), paragraph2.getVertexes());
		
		
		List<String> lines3 = populateLinesOfAParagraph3();
		List<Vertex> vertexes3 = populateListVertex3();

		ParagraphText paragraph3 = new ParagraphText(vertexes3, lines3);

		List<ParagraphText> paragraphs3 = navigationOnLeft3.getParagraphs();

		assertEquals(paragraphs3.get(6).getLines(), paragraph3.getLines());
		assertEquals(paragraphs3.get(6).getVertexes(), paragraph3.getVertexes());
	}

	@Test
	public void TestBlockHasLink() throws Exception {

		List<String> lines1 = populateLinesOfAParagraph1();

		LinkedHashMap<String, String> links1 = navigationOnLeft1.getLinksFromPage();

		for (String lines : lines1) {
			assertTrue(links1.keySet().contains(lines));
		}

		LinkedHashMap<String, String> links2 = navigationOnLeft2.getLinksFromPage();

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
	public void TestIsTheQuantityOfLinksByWordsEnoughInAPieceOfWebPage() {

		List<ParagraphText> paragraphs = new ArrayList<>();
		List<String> blockLines = populateLinesOfAParagraph1();
		paragraphs.add(new ParagraphText(populateListVertex1(), blockLines));

		int qtdOfLinks1 = 3;

		boolean result1 = navigationOnLeft1.isTheQuantityOfLinksByWordsEnough(paragraphs, qtdOfLinks1);

		assertTrue(result1);
	}

	@Test
	public void TestIsTheNavigationOnTheLeftSide() throws Exception {
			
		boolean response1 = navigationOnLeft1.isTheNavigationOnTheLeftSide();
		assertTrue(response1);

		boolean response2 = navigationOnLeft2.isTheNavigationOnTheLeftSide();
		assertTrue(response2);			
		
		boolean response3 = navigationOnLeft3.isTheNavigationOnTheLeftSide();
		assertFalse(response3);
	}
	
	@Test
	public void TestParagraphsOnTheMiddleLeftOfTheImage() {

	    List<String> lines1 = populateLinesOfAParagraph1();
		List<Vertex> vertexes1 = populateListVertex2();

		List<ParagraphText> expected1 = new ArrayList<>();
		expected1.add(new ParagraphText(vertexes1, lines1));
		
		List<String> linesFalse1 = new ArrayList<>();
		linesFalse1.add("Ciência");
		List<Vertex> vertexesFalse1 = populateListVertexFalse1();		
		
		List<ParagraphText> expectedFalse1 = new ArrayList<>();
		expectedFalse1.add(new ParagraphText(vertexesFalse1, linesFalse1));
		
		List<ParagraphText> response1 = navigationOnLeft1.getParagraphsOnTheMiddleLeft();
		
		assertTrue(expected1.get(0).getLines().contains(response1.get(1).getLines().get(0)));					
			
		for (String lineResponse : response1.get(1).getLines()) {	
			assertFalse(expectedFalse1.get(0).getLines().contains(response1));			
		}
		
	}
	
	@Test
	public void TestGetItemsOnNavBarTag() {
		
		List<String> expectedItemsOnNavBarTag = new ArrayList<String>();
		expectedItemsOnNavBarTag.add("Perguntas");
		expectedItemsOnNavBarTag.add("Tags");
		expectedItemsOnNavBarTag.add("Usuários");
		
		List<String> resultItemsOnNavBarTag = navigationOnLeft3.getItemsOnNavBarTag();
		
		for (String item : expectedItemsOnNavBarTag) {
			assertTrue(resultItemsOnNavBarTag.contains(item));
		}
	}
	
	@Test
	public void TestIsTheNavBarTagOnLeftSide() {
		
		boolean result1 = navigationOnLeft3.isTheNavBarTagOnLeftSide();		
		assertFalse(result1);
	}
	
	@Test
	public void TestAreThereEnoughItemsOnNavBarTagOnLeftSide() {
		
		List<String> expectedItemsOnNavBarTag = new ArrayList<String>();
		expectedItemsOnNavBarTag.add("Perguntas");
		expectedItemsOnNavBarTag.add("Tags");
		expectedItemsOnNavBarTag.add("Usuários");
		
		boolean result1 = navigationOnLeft3.AreThereEnoughItemsOnNavBarTagOnLeftSide(1, expectedItemsOnNavBarTag);		
		assertFalse(result1);
	}

	@Test
	public void TestAreThereToManyOptionsOnNavigation() throws Exception {
		int qtdResult1 = navigationOnLeft1.getQuantityOfItemsOnMiddleLeft();
		assertEquals(16, qtdResult1);

		int qtdResult2 = navigationOnLeft2.getQuantityOfItemsOnMiddleLeft();
		assertEquals(7, qtdResult2);
	}

	@Test
	public void TestItemsOnNavigation() {
		List<String> itemsExpected1 = populateLinesOfAParagraph1();

		List<String> itemsResult = navigationOnLeft1.getItems();

		for (String item : itemsResult) {
			assertTrue(itemsExpected1.contains(item));
		}
	}

	@Test
	public void TestNavigationItemsOnAlphabeticalOrder() {
		
		List<List<String>> expectedAlphabeticalOrder1 = populateNavigationItemsWithWordsOnAlphabeticalOrder1();		
		List<String> items = populateLinesOfAParagraph1();
		
		NavigationOnLeft nv1 = spy(NavigationOnLeft.class);
		when(nv1.getItems()).thenReturn(items);
		nv1.setItemsOnAlphabeticalOrder();
		
		List<List<String>> result1 = nv1.getItemsOnAlphabeticalOrder();
		assertEquals(expectedAlphabeticalOrder1, result1);
		
		
		List<List<String>> expectedAlphabeticalOrder2 = populateNavigationItemsWithWordsOnAlphabeticalOrder2();		
		List<String> items2 = populateNavigationItems();
		
		NavigationOnLeft nv2 = spy(NavigationOnLeft.class);
		when(nv2.getItems()).thenReturn(items2);
		nv2.setItemsOnAlphabeticalOrder();
		
		List<List<String>> result2 = nv2.getItemsOnAlphabeticalOrder();
		assertEquals(expectedAlphabeticalOrder2, result2);			
	}
	
	@Test
	public void TestAreThereEnoughItemsOnAlphabeticalOrder() {
			
		List<String> items = populateNavigationItems();
		
		NavigationOnLeft nv1 = spy(NavigationOnLeft.class);
		when(nv1.getItems()).thenReturn(items);
		when(nv1.getQuantityOfItemsOnMiddleLeft()).thenReturn(items.size());
		nv1.setItemsOnAlphabeticalOrder();
		
		boolean result1 = nv1.areThereEnoughItemsOnAlphabeticalOrder();
		assertTrue(result1);
				
			
		List<String> items2 = populateLinesOfAParagraph1();
		
		NavigationOnLeft nv2 = spy(NavigationOnLeft.class);
		when(nv2.getItems()).thenReturn(items2);		
		when(nv2.getQuantityOfItemsOnMiddleLeft()).thenReturn(items2.size());
		nv2.setItemsOnAlphabeticalOrder();
		
		boolean result2 = nv2.areThereEnoughItemsOnAlphabeticalOrder();
		assertFalse(result2);
		
		
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
	
	private List<List<String>> populateNavigationItemsWithWordsOnAlphabeticalOrder1(){
		List<List<String>> blocks = new ArrayList<>();
						
		List<String> block = new ArrayList<>();
		block.add("Informar um erro");
		block.add("Loja da Wikipédia");
		blocks.add(block);
			
		return blocks;		
	}
	
	private List<String> populateLinesOfAParagraph2() {
		List<String> lines2 = new ArrayList<>();
		lines2.add("Páginas do site");
		lines2.add("Curso atual");
		return lines2;
	}
	
	private List<String> populateNavigationItems() {
		List<String> lines = new ArrayList<>();
		lines.add("Página inicial");
		lines.add("Páginas do site");
		lines.add("Curso atual");
		lines.add("Dire, fare, partire! Italiano para brasileiros");
		lines.add("Participantes");
		lines.add("Benvenuti");
		lines.add("Lezione 1");
		lines.add("Lezione 2");
		lines.add("Lezione 3");
		lines.add("Lezione 4");
		lines.add("Lezione 5");
		lines.add("Lezione 6");
		lines.add("Lezione 7");
		lines.add("Lezione 8");		
		return lines;	
	}
	
	private List<List<String>> populateNavigationItemsWithWordsOnAlphabeticalOrder2(){
		List<List<String>> blocks = new ArrayList<>();
		
		List<String> block = new ArrayList<>();
		block.add("Curso atual");
		block.add("Dire, fare, partire! Italiano para brasileiros");
		blocks.add(block);
		
		block = new ArrayList<>();
		block.add("Lezione 1");
		block.add("Lezione 2");
		block.add("Lezione 3");
		block.add("Lezione 4");
		block.add("Lezione 5");
		block.add("Lezione 6");
		block.add("Lezione 7");
		block.add("Lezione 8");
		blocks.add(block);
		
		block = new ArrayList<>();
		block.add("Página inicial");
		block.add("Páginas do site");
		blocks.add(block);
		
		return blocks;		
	}
	
	private List<String> populateLinesOfAParagraph3() {
		List<String> lines2 = new ArrayList<>();
		lines2.add("Qualquer pes");
		lines2.add("pode fazer u");
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
	
	private List<Vertex> populateListVertex3() {
		List<Vertex> vertexes = new ArrayList<>();
		vertexes.add(Vertex.newBuilder().setX(546).setY(237).build());
		vertexes.add(Vertex.newBuilder().setX(643).setY(237).build());
		vertexes.add(Vertex.newBuilder().setX(643).setY(274).build());
		vertexes.add(Vertex.newBuilder().setX(546).setY(274).build());
		return vertexes;
	}
	
	private List<Vertex> populateListVertexFalse1() {
		List<Vertex> vertexes = new ArrayList<>();
		vertexes.add(Vertex.newBuilder().setX(502).setY(402).build());
		vertexes.add(Vertex.newBuilder().setX(540).setY(402).build());
		vertexes.add(Vertex.newBuilder().setX(540).setY(419).build());
		vertexes.add(Vertex.newBuilder().setX(502).setY(419).build());
		return vertexes;
	}

}

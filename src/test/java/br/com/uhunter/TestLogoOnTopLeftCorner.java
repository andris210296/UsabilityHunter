package br.com.uhunter;

import static org.junit.Assert.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import br.com.uhunter.logo.LogoIdentification;
import br.com.uhunter.utils.GoogleVision;

public class TestLogoOnTopLeftCorner {

	@Test
	public void TestIsThereALogoOnTopLeftCorner() throws Exception {
		String url = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";
		File file = new File("imgTest/wikipediaMatrix_0_0.jpg");
		InputStream inputStream = new FileInputStream(file);
		
		LogoIdentification logoIdentification = new LogoIdentification();
		String result = logoIdentification.isThereALogoOnTopLeftCorner(url, inputStream);
		
		assertEquals("Wikipedia", result);
		assertTrue(result != null);
	}
	
	@Test
	public void TestTitleWebPage() throws Exception {
		String url1 = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";		
		String expectedTitle1 = "Wikipedia";						
		String result1 = LogoIdentification.getWebPageTitleFromHtml(url1);		
		String title1 = setTitleAndStringsOnLogoIdentification(result1, expectedTitle1);
		assertEquals(expectedTitle1, title1);
		
		String url2 = "https://pt.stackoverflow.com/";		
		String expectedTitle2 = "Stackoverflow";						
		String result2 = LogoIdentification.getWebPageTitleFromHtml(url2);		
		String title2 = setTitleAndStringsOnLogoIdentification(result2, expectedTitle2);
		assertEquals(expectedTitle2, title2);		
	}
	
	/**
	 * This test verifies if the GoogleVision API works when you want to find out
	 * logos inside of an image.
	 * 
	 * @throws Exception
	 */
	@Test
	public void TestGetCorrectLogoFromGoogleVision() throws Exception {

		String logo1 = "Google";
		List<String> resultsLogo1 = getResultsFromGoogleVisionDetectLogo("imgTest/google.jpg");
		assertEquals(logo1, getLogoAfterVerificationWithUrlTitle(logo1, resultsLogo1));

		String logo2 = "Wikipedia";
		List<String> resultsLogo2 = getResultsFromGoogleVisionDetectLogo("imgTest/wikipedia.png");
		assertEquals(logo2, getLogoAfterVerificationWithUrlTitle(logo2, resultsLogo2));

		String logo3 = "StackOverflow";
		List<String> resultsLogo3 = getResultsFromGoogleVisionDetectLogo("imgTest/stackoverflow.jpg");
		assertEquals(logo3, getLogoAfterVerificationWithUrlTitle(logo3, resultsLogo3));
		
		String logo4 = "USP";
		List<String> resultsLogo4 = getResultsFromGoogleVisionDetectLogo("imgTest/uspPage.jpg");
		//assertEquals(logo4, getLogoAfterVerificationWithUrlTitle(logo4, resultsLogo4));

	}

	private List<String> getResultsFromGoogleVisionDetectLogo(String fileUrl) throws Exception {
		File fileLogo = new File(fileUrl);
		InputStream imputStreamLogo1 = new FileInputStream(fileLogo);
		return GoogleVision.detectLogo(imputStreamLogo1);
	}

	private String getLogoAfterVerificationWithUrlTitle(String logo, List<String> logos) {
		LogoIdentification logoIdentification = new LogoIdentification();
		logoIdentification.setPageTitle(logo);
		return logoIdentification.isStringInsideOfTheTitle(logos);
	}

	/**
	 * This test verifies if the method isStringInsideOfTheTitle is doing the right
	 * assert when you give some string and some url title. This means that when
	 * Google Vision API returns some possible logos, the method
	 * isStringInsideOfTheTitle must be able to verify correctly if the url title
	 * has one of possible logos, ignoring any different character or spacing.
	 */
	@Test
	public void TestIfTheStringIsInsideOfASentence() {

		String title1 = "title";
		String string1 = "title";
		String logo1 = setTitleAndStringsOnLogoIdentification(title1, string1);
		assertEquals(string1, logo1);

		String title2 = "title";
		String string2 = "Title";
		String logo2 = setTitleAndStringsOnLogoIdentification(title2, string2);
		assertEquals(string2, logo2);

		String title3 = "Title";
		String string3 = "title";
		String logo3 = setTitleAndStringsOnLogoIdentification(title3, string3);
		assertEquals(string3, logo3);

		String title4 = "Title with some text";
		String string4 = "title";
		String logo4 = setTitleAndStringsOnLogoIdentification(title4, string4);
		assertEquals(string4, logo4);

		String title5 = "Some text and títle";
		String string5 = "title";
		String logo5 = setTitleAndStringsOnLogoIdentification(title5, string5);
		assertEquals(string5, logo5);

		String title6 = "Some text and title";
		String string6 = "títle";
		String logo6 = setTitleAndStringsOnLogoIdentification(title6, string6);
		assertEquals(string6, logo6);

		String title7 = "Some text and title";
		String string7 = "títle";
		String string72 = "another possibility";
		String logo7 = setTitleAndStringsOnLogoIdentification(title7, string72, string7);
		assertEquals(string7, logo7);
	}

	@Test
	public void TestIfTheSenteceIsInsideOfAnotherSentence() {
		String title1 = "title with some other texts";
		String string1 = "Titel";
		String logo1 = setTitleAndStringsOnLogoIdentification(title1, string1);
		assertEquals(string1, logo1);

		String title2 = "some other texts and stackoverflow";
		String string2 = "stack";
		String logo2 = setTitleAndStringsOnLogoIdentification(title2, string2);
		assertNotEquals(string2, logo2);

		String title3 = " texts stackoverflow texts";
		String string3 = "staoverflow";
		String logo3 = setTitleAndStringsOnLogoIdentification(title3, string3);
		assertEquals(string3, logo3);
		
		String title4 = " texts stack overflow texts";
		String string4 = "stackoverflow";
		String logo4 = setTitleAndStringsOnLogoIdentification(title4, string4);
		assertEquals(string4, logo4);

	}

	private String setTitleAndStringsOnLogoIdentification(String title, String... string) {
		LogoIdentification logoIdentification = new LogoIdentification();

		List<String> strings = new ArrayList<String>();
		for (int i = 0; i < string.length; i++) {
			strings.add(string[i]);
		}

		logoIdentification.setPageTitle(title);
		return logoIdentification.isStringInsideOfTheTitle(strings);
	}

	@Test
	public void TestAreTheseStringsSimilar() {

		LogoIdentification logoIdentification = new LogoIdentification();

		String string1 = "wikkipedia";
		String string2 = "wikipedia";

		boolean response1 = logoIdentification.areTheseStringSimilar(string1, string2);
		assertTrue(response1);

		boolean response2 = logoIdentification.areTheseStringSimilar("stackoverflow", "staroverflow");
		assertTrue(response2);
		
		boolean response3 = logoIdentification.areTheseStringSimilar("stack overflow", "staroverflow");
		assertTrue(response3);

		boolean response4 = logoIdentification.areTheseStringSimilar("stackoverflow", "stackover");
		assertFalse(response4);
	}
	
	@Test
	public void TestWordsCombination() {

		LogoIdentification logoIdentification = new LogoIdentification();

		String string1 = "some random text";
		
		List<String> possibleSolution = new ArrayList<>();
		possibleSolution.add("some");
		possibleSolution.add("somerandom");
		possibleSolution.add("somerandomtext");
		possibleSolution.add("random");
		possibleSolution.add("randomtext");
		possibleSolution.add("text");

		List<String> result = logoIdentification.wordsCombination(string1);
		
		assertEquals(possibleSolution, result);
	}

}

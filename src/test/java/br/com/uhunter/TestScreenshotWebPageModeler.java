package br.com.uhunter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.imageio.ImageIO;

import org.junit.Test;

public class TestScreenshotWebPageModeler {

	@Test
	public void TestTakeScreenshot() throws Exception {
		String url1 = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";

		ScreenshotWebPageModeler screenshotWebPageModeler = new ScreenshotWebPageModeler();
		InputStream inputStream = screenshotWebPageModeler.takeScreenshot(url1);

		assertNotNull(inputStream);
	}

	@Test
	public void TestScreenshotPieces() throws IOException {
		File file = new File("imgTest/wikipediaScreenshot.jpg");

		BufferedImage bufferedImage = ImageIO.read(file);

		ScreenshotWebPageModeler screenshotWebPageModeler = new ScreenshotWebPageModeler();
		screenshotWebPageModeler.setQuantityOfPieces(bufferedImage);
		assertEquals(2, screenshotWebPageModeler.getHorizontalPieces());
		assertEquals(5, screenshotWebPageModeler.getVerticalPieces());		
	}
	
	@Test
	public void TestScreenshotMatrix() throws Exception {		
		File file = new File("imgTest/wikipediaScreenshot.jpg");

		InputStream inputStream = new FileInputStream(file);
		
		ScreenshotWebPageModeler screenshotWebPageModeler = spy(ScreenshotWebPageModeler.class);
		screenshotWebPageModeler.setImputStream(inputStream);
		screenshotWebPageModeler.setVerticalPieces(5);
		screenshotWebPageModeler.setHorizontalPieces(2);
		
		BufferedImage[][] screenshotMatrix = screenshotWebPageModeler.getImagePieces();
		
		assertEquals(2, screenshotMatrix[0].length);
		assertEquals(5, screenshotMatrix.length);	

		LogoIdentification logoIdentification = new LogoIdentification();
		InputStream inputStream2 = ImageUtils.bufferedImageToInputStream(screenshotMatrix[0][0]);
		boolean result = logoIdentification.isThereALogoOnTopLeftCorner("https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal", inputStream2);
		
		assertTrue(result);
		
	}

}

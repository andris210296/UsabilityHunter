package br.com.uhunter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.imageio.ImageIO;

import org.junit.Test;


import br.com.uhunter.logo.LogoIdentification;
import br.com.uhunter.utils.ImageUtils;
import br.com.uhunter.utils.ScreenshotWebPageModeler;

public class TestScreenshotWebPageModeler {

	@Test
	public void TestTakeScreenshot() throws Exception {
		String url1 = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";

		ScreenshotWebPageModeler screenshotWebPageModeler = new ScreenshotWebPageModeler();
		InputStream inputStream = ImageUtils.byteArrayToInputStream(screenshotWebPageModeler.takeScreenshot(url1));

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
		
		ScreenshotWebPageModeler screenshotWebPageModeler = spy(ScreenshotWebPageModeler.class);
		screenshotWebPageModeler.setByteImage(Files.readAllBytes(file.toPath()));
		screenshotWebPageModeler.setBufferedImage(ImageUtils.byteArrayToBufferedImage(Files.readAllBytes(file.toPath())));
		screenshotWebPageModeler.setVerticalPieces(5);
		screenshotWebPageModeler.setHorizontalPieces(2);
		
		byte[][][] screenshotMatrix = screenshotWebPageModeler.getByteImageMatrix();
		
		assertEquals(2, screenshotMatrix[0].length);
		assertEquals(5, screenshotMatrix.length);	

		LogoIdentification logoIdentification = new LogoIdentification();
		byte[] byteImage2 = screenshotMatrix[0][0];
		String result = logoIdentification.isThereALogoOnTopLeftCorner("https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal", byteImage2);
		
		assertEquals("Wikipedia", result);
		assertTrue(result != null);
		
	}
		
	@Test
	public void TestGetImagePiece() throws Exception {

		String url = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";		
		LogoIdentification logoIdentification = new LogoIdentification();						
		ScreenshotWebPageModeler screenshotWebPageModeler = new ScreenshotWebPageModeler(url);
		byte[] byteImage = screenshotWebPageModeler.getByteImagePiece(0,0);		
		String result = logoIdentification.isThereALogoOnTopLeftCorner(url, byteImage);		
		assertEquals("Wikipedia", result);	
		
		String url2 = "https://pt.stackoverflow.com/";		
		LogoIdentification logoIdentification2 = new LogoIdentification();						
		ScreenshotWebPageModeler screenshotWebPageModeler2 = new ScreenshotWebPageModeler(url2);
		byte[] byteImage2 = screenshotWebPageModeler2.getByteImagePiece(0,0);		
		String result2 = logoIdentification2.isThereALogoOnTopLeftCorner(url2, byteImage2);		
		assertEquals("StackOverflow", result2);	
	}
	
	@Test
	public void TestMatrixByteArray() throws Exception {
		
		File file = new File("imgTest/wikipediaScreenshot.jpg");
		
		ScreenshotWebPageModeler screenshotWebPageModeler = spy(ScreenshotWebPageModeler.class);
		screenshotWebPageModeler.setByteImage(Files.readAllBytes(file.toPath()));
		screenshotWebPageModeler.setVerticalPieces(5);
		screenshotWebPageModeler.setHorizontalPieces(2);
		
		byte[][][] screenshotMatrix = screenshotWebPageModeler.getBytesImagePieces();
		
		assertEquals(2, screenshotMatrix[0].length);
		assertEquals(5, screenshotMatrix.length);	

		LogoIdentification logoIdentification = new LogoIdentification();
		byte[] byteImage2 = screenshotMatrix[0][0];
		String result = logoIdentification.isThereALogoOnTopLeftCorner("https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal", byteImage2);
		
		assertEquals("Wikipedia", result);
		assertTrue(result != null);
		
		
	}

}

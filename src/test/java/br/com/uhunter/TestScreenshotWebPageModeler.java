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
		String result = logoIdentification.isThereALogoOnTopLeftCorner("https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal", inputStream2);
		
		assertEquals("Wikipedia", result);
		assertTrue(result != null);
		
	}
	
	@Test
	public void TestBufferedMatrixToInputStreamMatrix() throws Exception {
		
		File file = new File("imgTest/wikipediaScreenshot.jpg");
		
		BufferedImage[][] bufferedImages = new BufferedImage[2][2];
		bufferedImages[0][0] = ImageIO.read(file);
		bufferedImages[0][1] = ImageIO.read(file);
		bufferedImages[1][0] = ImageIO.read(file);
		bufferedImages[1][1] = ImageIO.read(file);
		
		ScreenshotWebPageModeler screenshotWebPageModeler = new ScreenshotWebPageModeler();		
		InputStream[][] inputStreams = screenshotWebPageModeler.bufferedMatrixToInputStreamMatrix(bufferedImages);
		
		for(int i = 0; i < inputStreams.length; i ++)
		{
			for(int j = 0; j < inputStreams[0].length; j ++)
			{
				assertTrue(inputStreams[i][j] != null);				
			}			
		}				
	}
	
	@Test
	public void TestGetImagePiece() throws Exception {

		String url = "https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal";		
		LogoIdentification logoIdentification = new LogoIdentification();						
		ScreenshotWebPageModeler screenshotWebPageModeler = new ScreenshotWebPageModeler(url);
		InputStream inputStream = screenshotWebPageModeler.getImagePiece(0,0);		
		String result = logoIdentification.isThereALogoOnTopLeftCorner(url, inputStream);		
		assertEquals("Wikipedia", result);	
		
		String url2 = "https://pt.stackoverflow.com/";		
		LogoIdentification logoIdentification2 = new LogoIdentification();						
		ScreenshotWebPageModeler screenshotWebPageModeler2 = new ScreenshotWebPageModeler(url2);
		InputStream inputStream2 = screenshotWebPageModeler2.getImagePiece(0,0);		
		String result2 = logoIdentification2.isThereALogoOnTopLeftCorner(url2, inputStream2);		
		assertEquals("StackOverflow", result2);	
	}	

}

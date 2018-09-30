package br.com.uhunter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.cloud.vision.v1.Vertex;

import br.com.uhunter.utils.GoogleVision;
import br.com.uhunter.utils.ImageUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.io.*;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

public class TestImageUtils {
	
	@Test
	public void TestGetTextFromAPieceOfImage() throws Exception {
				
		File file = new File("imgTest/wikipediaNavigation.jpg");
		InputStream inputStream = new FileInputStream(file);
		
		List<String> strings = new ArrayList<>();
		strings.add("WIKIPEDIA");
		strings.add("Um milhao de artigos!");
				
		List<Vertex> vertexes = new ArrayList<>();
		vertexes.add(Vertex.newBuilder().setX(27).setY(120).build());
		vertexes.add(Vertex.newBuilder().setX(148).setY(120).build());
		vertexes.add(Vertex.newBuilder().setX(148).setY(157).build());
		vertexes.add(Vertex.newBuilder().setX(27).setY(157).build());
		
		InputStream inputStreamPiece = ImageUtils.getPiece(ImageUtils.inputStreamToBufferedImage(inputStream), vertexes);
		
		List<String> result = GoogleVision.detectText(inputStreamPiece);
		
		assertEquals(strings, result);
	}
	
	@Test
	public void TestGetTextFromAPieceOfImageWhenTheSquareIsRotated() throws Exception {
				
		File file = new File("imgTest/wikipediaNavigation.jpg");
		InputStream inputStream = new FileInputStream(file);
		
		List<String> strings = new ArrayList<>();
				
		List<Vertex> vertexes = new ArrayList<>();
		vertexes.add(Vertex.newBuilder().setX(51).setY(61).build());
		vertexes.add(Vertex.newBuilder().setX(107).setY(41).build());
		vertexes.add(Vertex.newBuilder().setX(111).setY(54).build());
		vertexes.add(Vertex.newBuilder().setX(56).setY(74).build());
		
		InputStream inputStreamPiece = ImageUtils.getPiece(ImageUtils.inputStreamToBufferedImage(inputStream), vertexes);
		
		List<String> result = GoogleVision.detectText(inputStreamPiece);
		
		assertEquals(strings, result);
		
	}
	
	@Test
	public void TestRasterFormatException() throws Exception {
		
		List<String> strings1 = new ArrayList<>();
		strings1.add("Dire, fare, pa");
				
		List<Vertex> vertexes2 = new ArrayList<>();
		vertexes2.add(Vertex.newBuilder().setX(341).setY(130).build());
		vertexes2.add(Vertex.newBuilder().setX(423).setY(130).build());
		vertexes2.add(Vertex.newBuilder().setX(423).setY(148).build());
		vertexes2.add(Vertex.newBuilder().setX(341).setY(148).build());
		
		InputStream inputStreamPiece1 = ImageUtils.getPiece(ImageUtils.inputStreamToBufferedImage(new FileInputStream("imgTest/uspNavigation.jpg")), vertexes2);
		
		List<String> result1 = GoogleVision.detectText(inputStreamPiece1);
		
		assertEquals(strings1, result1);
				
		
		List<String> strings2 = new ArrayList<>();
		strings2.add("Qualquer pes");
		strings2.add("pode fazer u");
		
		List<Vertex> vertexes1 = new ArrayList<>();
		vertexes1.add(Vertex.newBuilder().setX(546).setY(237).build());
		vertexes1.add(Vertex.newBuilder().setX(643).setY(237).build());
		vertexes1.add(Vertex.newBuilder().setX(643).setY(274).build());
		vertexes1.add(Vertex.newBuilder().setX(546).setY(274).build());
		
		InputStream inputStreamPiece2 = ImageUtils.getPiece(ImageUtils.inputStreamToBufferedImage(new FileInputStream("imgTest/stackoverflow.jpg")), vertexes1);
		
		List<String> result2 = GoogleVision.detectText(inputStreamPiece2);
		
		assertEquals(strings2, result2);
		
		
		List<String> strings3 = new ArrayList<>();
		
		List<Vertex> vertexes3 = new ArrayList<>();
		vertexes3.add(Vertex.newBuilder().setX(250).setY(482).build());
		vertexes3.add(Vertex.newBuilder().setX(253).setY(481).build());
		vertexes3.add(Vertex.newBuilder().setX(256).setY(495).build());
		vertexes3.add(Vertex.newBuilder().setX(253).setY(496).build());
		
		InputStream inputStreamPiece3 = ImageUtils.getPiece(ImageUtils.inputStreamToBufferedImage(new FileInputStream("imgTest/stackoverflow.jpg")), vertexes3);
		
		List<String> result3 = GoogleVision.detectText(inputStreamPiece3);
		
		assertEquals(strings3, result3);
		
		
		List<String> strings4 = new ArrayList<>();
		strings4.add("N�O � NECESS�RI");
		strings4.add("Dire, fare, partirel� uma produ��o do Progra");
		
		List<Vertex> vertexes4 = new ArrayList<>();
		vertexes4.add(Vertex.newBuilder().setX(366).setY(358).build());
		vertexes4.add(Vertex.newBuilder().setX(644).setY(358).build());
		vertexes4.add(Vertex.newBuilder().setX(644).setY(522).build());
		vertexes4.add(Vertex.newBuilder().setX(366).setY(522).build());
		
		InputStream inputStreamPiece4 = ImageUtils.getPiece(ImageUtils.inputStreamToBufferedImage(new FileInputStream("imgTest/uspPage.jpg")), vertexes4);
		
		List<String> result4 = GoogleVision.detectText(inputStreamPiece4);
		
		for (String string : strings4) {
			assertTrue(result4.contains(string));
		}
		
		
		List<String> strings5 = new ArrayList<>();
		strings5.add("TSP");
		
		List<Vertex> vertexes5 = new ArrayList<>();
		vertexes5.add(Vertex.newBuilder().setX(31).setY(-1).build());
		vertexes5.add(Vertex.newBuilder().setX(157).setY(-1).build());
		vertexes5.add(Vertex.newBuilder().setX(157).setY(48).build());
		vertexes5.add(Vertex.newBuilder().setX(31).setY(48).build());
		
		InputStream inputStreamPiece5 = ImageUtils.getPiece(ImageUtils.inputStreamToBufferedImage(new FileInputStream("imgTest/uspPage.jpg")), vertexes5);
		
		List<String> result5 = GoogleVision.detectText(inputStreamPiece5);
		
		for (String string : strings5) {
			assertTrue(result5.contains(string));
		}
		
		List<String> strings6 = new ArrayList<>();
		strings6.add("A Wikip�dia em Lingua Portugues");
		
		List<Vertex> vertexes6 = new ArrayList<>();
		vertexes6.add(Vertex.newBuilder().setX(442).setY(227).build());
		vertexes6.add(Vertex.newBuilder().setX(689).setY(225).build());
		vertexes6.add(Vertex.newBuilder().setX(689).setY(244).build());
		vertexes6.add(Vertex.newBuilder().setX(442).setY(246).build());
		
		InputStream inputStreamPiece6 = ImageUtils.getPiece(ImageUtils.inputStreamToBufferedImage(new FileInputStream("imgTest/wikipediaMatrix_0_0.jpg")), vertexes6);
		
		List<String> result6 = GoogleVision.detectText(inputStreamPiece6);
		
		assertTrue(result6.isEmpty());
		
	}

}

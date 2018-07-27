package br.com.uhunter;

import static org.junit.Assert.assertEquals;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.cloud.vision.v1.Vertex;

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
	public void TestGetTextFromAPieceOfImageWhenTheVertexIsSmall() throws Exception {
		
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
	}
	
}

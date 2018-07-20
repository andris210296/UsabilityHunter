package br.com.uhunter;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.*;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.cloud.vision.v1.Vertex;

public class ImageUtils {

	public static InputStream getPiece(BufferedImage bfImage, List<Vertex> vertexes) throws Exception{

		int widthCut = Math.abs(vertexes.get(3).getX() - vertexes.get(1).getX());
		int heightCut = Math.abs(vertexes.get(3).getY() - vertexes.get(1).getY());

		try {

			return bufferedImageToInputStream(bfImage.getSubimage(vertexes.get(0).getX(), vertexes.get(0).getY(), widthCut, heightCut));

		} catch (RasterFormatException e) {
			
			return bufferedImageToInputStream(bfImage.getSubimage(vertexes.get(0).getX() - 1, vertexes.get(0).getY(), widthCut - 1, heightCut));
		} 
		
	}

	public static InputStream bufferedImageToInputStream(BufferedImage bufferedImage) throws Exception {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "jpg", baos);
		byte[] imagem = baos.toByteArray();

		return new ByteArrayInputStream(imagem);
	}

	public static BufferedImage inputStreamToBufferedImage(InputStream inputStream) throws Exception {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];
		int len = 0;

		try {
			while ((len = inputStream.read(buffer)) != -1) {
				baos.write(buffer, 0, len);
			}

			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		byte[] b = baos.toByteArray();
		BufferedImage img = ImageIO.read(new ByteArrayInputStream(b));
		return img;
	}
	
	public static void inputStreamToFile(InputStream inputStream) throws Exception {

		FileOutputStream output = new FileOutputStream("C:/Users/andri/OneDrive/Documentos/Imgs/image.jpg");
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];
		int len = 0;
		while ((len = inputStream.read(buffer)) != -1) {
			output.write(buffer, 0, len);
		}
		output.close();
	}

}

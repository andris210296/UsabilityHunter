package br.com.uhunter.utils;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.appengine.repackaged.com.google.common.io.ByteStreams;
import com.google.cloud.vision.v1.Vertex;
import com.google.common.io.ByteSource;

public class ImageUtils {

	public static InputStream getPiece(BufferedImage bfImage, List<Vertex> vertexes) throws Exception {

		int widthCut = Math.abs(vertexes.get(3).getX() - vertexes.get(1).getX());
		int heightCut = Math.abs(vertexes.get(3).getY() - vertexes.get(1).getY());

		try {
			return bufferedImageToInputStream(
					bfImage.getSubimage(vertexes.get(0).getX(), vertexes.get(0).getY(), widthCut, heightCut));

		} catch (RasterFormatException e1) {

			try {
				return bufferedImageToInputStream(bfImage.getSubimage(vertexes.get(0).getX() - 1,
						vertexes.get(0).getY(), widthCut - 1, heightCut));

			} catch (RasterFormatException e2) {
				try {
					return bufferedImageToInputStream(bfImage.getSubimage(vertexes.get(0).getX() - 2,
							vertexes.get(0).getY(), widthCut - 2, heightCut));
				} catch (RasterFormatException e3) {
					try {
						return bufferedImageToInputStream(bfImage.getSubimage(vertexes.get(0).getX() + 1,
								vertexes.get(0).getY(), widthCut + 1, heightCut));
					} catch (RasterFormatException e4) {
						try {
							return bufferedImageToInputStream(bfImage.getSubimage(vertexes.get(0).getX(),
									vertexes.get(0).getY() - 1, widthCut, heightCut - 1));
						} catch (RasterFormatException e5) {
							try {
								return bufferedImageToInputStream(bfImage.getSubimage(vertexes.get(0).getX() - 2,
										vertexes.get(0).getY() - 1, widthCut - 2, heightCut - 1));
							} catch (RasterFormatException e6) {
								try {
									return bufferedImageToInputStream(bfImage.getSubimage(vertexes.get(0).getX(),
											vertexes.get(0).getY() + 1, widthCut, heightCut + 1));
								} catch (RasterFormatException e7) {
									return bufferedImageToInputStream(bfImage.getSubimage(vertexes.get(0).getX() - 3,
											vertexes.get(0).getY(), widthCut - 3, heightCut));
								}

							}
						}
					}
				}

			}
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
		int bufferSize = inputStream.available();
		byte[] buffer = new byte[bufferSize];
		int len = 0;

		try {
			if (bufferSize > 0) {
				while ((len = inputStream.read(buffer)) != -1) {
					baos.write(buffer, 0, len);
				}
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

		FileOutputStream output = new FileOutputStream("imgTest/image.jpg");
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];
		int len = 0;
		while ((len = inputStream.read(buffer)) != -1) {
			output.write(buffer, 0, len);
		}
		output.close();
	}

	public static void bufferedImageToFile(BufferedImage bufferedImage) throws Exception {
		File outputfile = new File("C:/Users/andri/OneDrive/Documentos/Imgs/image.jpg");
		ImageIO.write(bufferedImage, "jpg", outputfile);
	}

	public static List<BufferedImage> inputStreamToBufferedImage(List<InputStream> inputStreams) throws Exception {

		LinkedList<BufferedImage> bufferedImages = new LinkedList<>();

		for (InputStream inputStream : inputStreams) {
			bufferedImages.add(inputStreamToBufferedImage(inputStream));
		}

		return bufferedImages;
	}

	public static BufferedImage byteArrayToBufferedImage(byte[] byteImage) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(byteImage);
		return ImageIO.read(bais);
	}
	
	public static byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
		return ByteStreams.toByteArray(inputStream);
	}
	
	public static InputStream byteArrayToInputStream(byte[] byteImage) throws IOException {
		return ByteSource.wrap(byteImage).openStream();
	}
	
	public static byte[] bufferedImageToByteArray(BufferedImage bufferedImage) throws IOException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()){
            ImageIO.write(bufferedImage, "jpg", out);
            return out.toByteArray();
        }
	}

}

package br.com.uhunter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/** Generates tiny in-memory test images so tests never depend on files or the network. */
public final class TestImages {

	private TestImages() {
	}

	public static byte[] jpeg(int width, int height) throws IOException {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		g.dispose();
		return write(image, "jpg");
	}

	/** PNG with a transparent background; JPEG cannot encode alpha, so this exercises normalization. */
	public static byte[] transparentPng(int width, int height) throws IOException {
		return write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png");
	}

	private static byte[] write(BufferedImage image, String format) throws IOException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ImageIO.write(image, format, out);
			return out.toByteArray();
		}
	}
}

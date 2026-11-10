package br.com.uhunter.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

/**
 * Screenshot encode/decode helpers (JPEG/PNG + macOS WebP fallback via {@code sips}).
 */
public final class ImageUtils {

	private static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

	private ImageUtils() {
	}

	public static BufferedImage byteArrayToBufferedImage(byte[] byteImage) throws IOException {
		if (byteImage == null || byteImage.length == 0) {
			throw new IOException("Screenshot bytes are empty");
		}
		BufferedImage image = tryDecodeWithImageIo(byteImage);
		if (image != null) {
			return image;
		}
		if (isWebp(byteImage)) {
			image = tryDecodeWebpViaSips(byteImage);
			if (image != null) {
				return image;
			}
			throw new IOException("Could not decode WebP screenshot (native webp-imageio often lacks Apple Silicon). "
					+ "Fall back to JPEG final-screenshot or install a WebP-capable tool.");
		}
		throw new IOException("Could not decode screenshot bytes as an image (detected="
				+ detectImageKind(byteImage) + ", size=" + byteImage.length + ").");
	}

	/**
	 * Returns true when bytes look usable on this JVM.
	 * JPEG/PNG are assumed OK; WebP is probed because native plugins may be arch-incompatible.
	 */
	public static boolean isDecodable(byte[] byteImage) {
		if (byteImage == null || byteImage.length == 0) {
			return false;
		}
		String kind = detectImageKind(byteImage);
		if ("jpeg".equals(kind) || "png".equals(kind)) {
			return true;
		}
		try {
			return byteArrayToBufferedImage(byteImage) != null;
		} catch (Exception | LinkageError e) {
			log.debug("Screenshot bytes not decodable: {}", e.toString());
			return false;
		}
	}

	private static BufferedImage tryDecodeWithImageIo(byte[] byteImage) throws IOException {
		try {
			ImageIO.scanForPlugins();
			return ImageIO.read(new ByteArrayInputStream(byteImage));
		} catch (LinkageError e) {
			// webp-imageio 0.1.6 ships x86_64 only; after first failure, WebP clinit stays broken.
			log.debug("ImageIO native decoder unavailable (likely WebP x86_64-only): {}", e.toString());
			return null;
		}
	}

	/**
	 * macOS fallback: convert WebP → JPEG with {@code sips}, which is arm64-safe.
	 */
	private static BufferedImage tryDecodeWebpViaSips(byte[] webpBytes) {
		String os = System.getProperty("os.name", "").toLowerCase();
		if (!os.contains("mac")) {
			return null;
		}
		File webpFile = null;
		File jpegFile = null;
		try {
			webpFile = Files.createTempFile("uhunter-screenshot-", ".webp").toFile();
			jpegFile = Files.createTempFile("uhunter-screenshot-", ".jpg").toFile();
			Files.write(webpFile.toPath(), webpBytes);
			Process process = new ProcessBuilder(
					"sips", "-s", "format", "jpeg", webpFile.getAbsolutePath(),
					"--out", jpegFile.getAbsolutePath())
					.redirectErrorStream(true)
					.start();
			boolean finished = process.waitFor(20, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				log.warn("sips WebP conversion timed out");
				return null;
			}
			if (process.exitValue() != 0 || jpegFile.length() == 0) {
				log.warn("sips WebP conversion failed with exit={}", process.exitValue());
				return null;
			}
			log.debug("Decoded WebP screenshot via macOS sips ({} -> {} bytes)",
					webpBytes.length, jpegFile.length());
			return ImageIO.read(jpegFile);
		} catch (Exception e) {
			log.warn("sips WebP conversion unavailable: {}", e.toString());
			return null;
		} finally {
			if (webpFile != null) {
				webpFile.delete();
			}
			if (jpegFile != null) {
				jpegFile.delete();
			}
		}
	}

	public static boolean isWebp(byte[] bytes) {
		return "webp".equals(detectImageKind(bytes));
	}

	private static String detectImageKind(byte[] bytes) {
		if (bytes.length >= 12
				&& bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
				&& bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
			return "webp";
		}
		if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
			return "jpeg";
		}
		if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
			return "png";
		}
		return "unknown";
	}

	public static byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
		return ByteStreams.toByteArray(inputStream);
	}

	public static byte[] bufferedImageToByteArray(BufferedImage bufferedImage) throws IOException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ImageIO.write(bufferedImage, "jpg", out);
			return out.toByteArray();
		}
	}
}

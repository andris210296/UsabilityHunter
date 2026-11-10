package br.com.uhunter.screenshot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import br.com.uhunter.utils.ImageUtils;

/**
 * Immutable page screenshot, always normalized to opaque RGB / JPEG so Google Vision gets a supported
 * format. Cutting it into regions returns new {@code Screenshot}s and never changes this one.
 */
public final class Screenshot {

	/** Google Vision works best on images of at least this size; regions are cut so none is smaller. */
	private static final int VISION_MIN_WIDTH = 640;
	private static final int VISION_MIN_HEIGHT = 480;

	private final BufferedImage image;
	/** Encoded lazily: most regions are only ever measured or cut further, never sent anywhere. */
	private volatile byte[] jpeg;
	private volatile Screenshot topLeft;

	private Screenshot(BufferedImage image, byte[] jpeg) {
		this.image = image;
		this.jpeg = jpeg;
	}

	/**
	 * @param encodedImage PNG/JPEG/WebP bytes as returned by a {@code ScreenshotProvider}
	 * @throws IOException when the bytes cannot be decoded as an image
	 */
	public static Screenshot of(byte[] encodedImage) throws IOException {
		BufferedImage decoded = ImageUtils.byteArrayToBufferedImage(encodedImage);
		if (decoded == null) {
			throw new IOException("Screenshot image is null; capture/decode likely failed");
		}
		return new Screenshot(toOpaqueRgb(decoded), null);
	}

	public int width() {
		return image.getWidth();
	}

	public int height() {
		return image.getHeight();
	}

	/** JPEG bytes; a fresh copy, so callers cannot modify the screenshot. */
	public byte[] bytes() {
		return encoded().clone();
	}

	/** How many columns fit when every cell must stay at least Vision-sized (minimum 1). */
	public int suggestedColumns() {
		return Math.max(width() / VISION_MIN_WIDTH, 1);
	}

	/** How many rows fit when every cell must stay at least Vision-sized (minimum 1). */
	public int suggestedRows() {
		return Math.max(height() / VISION_MIN_HEIGHT, 1);
	}

	/**
	 * The above-the-fold corner both the logo and the navigation checks analyze: cell (0,0) of the
	 * {@linkplain #suggestedRows() suggested} grid. Computed once and shared.
	 */
	public Screenshot topLeft() {
		Screenshot cached = topLeft;
		if (cached == null) {
			cached = cell(suggestedRows(), suggestedColumns(), 0, 0);
			topLeft = cached;
		}
		return cached;
	}

	/** The four quarters in reading order: top-left, top-right, bottom-left, bottom-right. */
	public List<Screenshot> quadrants() {
		return List.of(cell(2, 2, 0, 0), cell(2, 2, 0, 1), cell(2, 2, 1, 0), cell(2, 2, 1, 1));
	}

	/**
	 * One cell of a {@code rows x columns} grid laid over the image (remainder pixels on the right and
	 * bottom edge are dropped, as the original slicing did).
	 */
	public Screenshot cell(int rows, int columns, int row, int column) {
		if (rows < 1 || columns < 1 || row < 0 || row >= rows || column < 0 || column >= columns) {
			throw new IllegalArgumentException(
					"Invalid cell (" + row + "," + column + ") for a " + rows + "x" + columns + " grid");
		}
		int cellWidth = width() / columns;
		int cellHeight = height() / rows;
		if (cellWidth < 1 || cellHeight < 1) {
			throw new IllegalArgumentException(
					"Cannot cut " + width() + "x" + height() + " image into a " + rows + "x" + columns + " grid");
		}
		BufferedImage region = copy(image.getSubimage(column * cellWidth, row * cellHeight, cellWidth, cellHeight));
		return new Screenshot(region, null);
	}

	private byte[] encoded() {
		byte[] current = jpeg;
		if (current == null) {
			try {
				current = ImageUtils.bufferedImageToByteArray(image);
			} catch (IOException e) {
				throw new IllegalStateException("Could not encode screenshot as JPEG", e);
			}
			jpeg = current;
		}
		return current;
	}

	/** JPEG has no alpha channel; flatten transparency onto white so PNGs encode instead of coming out empty. */
	private static BufferedImage toOpaqueRgb(BufferedImage source) {
		if (source.getType() == BufferedImage.TYPE_INT_RGB || source.getType() == BufferedImage.TYPE_3BYTE_BGR) {
			return source;
		}
		BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = rgb.createGraphics();
		try {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
			graphics.drawImage(source, 0, 0, null);
		} finally {
			graphics.dispose();
		}
		return rgb;
	}

	/** Detach a sub-image from its parent raster so the parent can be garbage collected. */
	private static BufferedImage copy(BufferedImage source) {
		BufferedImage detached = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = detached.createGraphics();
		try {
			graphics.drawImage(source, 0, 0, null);
		} finally {
			graphics.dispose();
		}
		return detached;
	}
}

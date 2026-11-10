package br.com.uhunter.screenshot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import br.com.uhunter.TestImages;
import br.com.uhunter.utils.ImageUtils;

class ScreenshotTest {

	@Test
	void suggestsGridSizedSoEveryCellStaysVisionSized() throws IOException {
		Screenshot screenshot = Screenshot.of(TestImages.jpeg(1400, 2500));

		assertEquals(2, screenshot.suggestedColumns());   // 1400 / 640
		assertEquals(5, screenshot.suggestedRows());      // 2500 / 480
	}

	@Test
	void smallImagesStillGetOneCell() throws IOException {
		Screenshot screenshot = Screenshot.of(TestImages.jpeg(300, 200));

		assertEquals(1, screenshot.suggestedColumns());
		assertEquals(1, screenshot.suggestedRows());
	}

	@Test
	void topLeftIsOneSuggestedCellAndIsShared() throws IOException {
		Screenshot screenshot = Screenshot.of(TestImages.jpeg(1400, 2500));

		Screenshot topLeft = screenshot.topLeft();

		assertEquals(1400 / 2, topLeft.width());
		assertEquals(2500 / 5, topLeft.height());
		assertSame(topLeft, screenshot.topLeft(), "logo and navigation must share one cut");
	}

	@Test
	void quadrantsSplitInReadingOrder() throws IOException {
		Screenshot screenshot = Screenshot.of(TestImages.jpeg(1000, 800));

		assertEquals(4, screenshot.quadrants().size());
		screenshot.quadrants().forEach(q -> {
			assertEquals(500, q.width());
			assertEquals(400, q.height());
		});
	}

	@Test
	void bytesAreDecodableJpegAndCannotBeUsedToMutateTheScreenshot() throws Exception {
		Screenshot screenshot = Screenshot.of(TestImages.jpeg(100, 100));

		byte[] first = screenshot.bytes();
		assertEquals(100, ImageUtils.byteArrayToBufferedImage(first).getWidth());
		first[0] = 0;
		first[1] = 0;

		byte[] second = screenshot.bytes();
		assertNotEquals(0, second[0]);
		assertArrayEquals(screenshot.bytes(), second);
	}

	@Test
	void transparentPngIsFlattenedInsteadOfEncodingToNothing() throws Exception {
		Screenshot screenshot = Screenshot.of(TestImages.transparentPng(50, 50));

		byte[] jpeg = screenshot.bytes();

		assertTrue(jpeg.length > 0, "an alpha image must still encode to JPEG");
		assertEquals(50, ImageUtils.byteArrayToBufferedImage(jpeg).getWidth());
	}

	@Test
	void rejectsInvalidCellsAndUndecodableBytes() throws IOException {
		Screenshot screenshot = Screenshot.of(TestImages.jpeg(100, 100));

		assertThrows(IllegalArgumentException.class, () -> screenshot.cell(2, 2, 2, 0));
		assertThrows(IllegalArgumentException.class, () -> screenshot.cell(0, 2, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> screenshot.cell(1000, 1, 0, 0));
		assertThrows(IOException.class, () -> Screenshot.of(new byte[] { 1, 2, 3 }));
		assertThrows(IOException.class, () -> Screenshot.of(new byte[0]));
	}
}

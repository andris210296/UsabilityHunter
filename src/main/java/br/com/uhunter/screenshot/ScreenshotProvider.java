package br.com.uhunter.screenshot;

/**
 * Strategy for capturing a webpage screenshot as encoded image bytes (JPEG/PNG/WebP).
 * The active implementation is chosen by {@code uhunter.screenshot-provider}
 * (see {@code ScreenshotConfig}).
 */
public interface ScreenshotProvider {

	byte[] capture(String url) throws Exception;
}

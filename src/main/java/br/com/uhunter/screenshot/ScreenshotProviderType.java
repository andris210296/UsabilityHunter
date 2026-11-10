package br.com.uhunter.screenshot;

/**
 * Values accepted by {@code uhunter.screenshot-provider}. Spring binds the property case-insensitively
 * ({@code pagespeed}, {@code PAGESPEED}), and an unknown value fails at startup with a clear message.
 */
public enum ScreenshotProviderType {
	/** Lighthouse screenshots from PageSpeed Insights (default). */
	PAGESPEED,
	/** Legacy PhantomJsCloud capture. */
	PHANTOMJS
}

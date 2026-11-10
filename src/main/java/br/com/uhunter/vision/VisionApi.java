package br.com.uhunter.vision;

import java.util.List;

/**
 * Image-analysis operations the heuristics need. Injected (not static) so checks can be tested with a
 * fake and so the Google client lifecycle is owned by Spring.
 */
public interface VisionApi {

	/** Brand names from Vision LOGO_DETECTION. */
	List<String> detectLogo(byte[] image) throws Exception;

	/** Non-empty OCR lines from Vision TEXT_DETECTION. */
	List<String> detectText(byte[] image) throws Exception;

	/**
	 * Web entities / best-guess labels for marks that logo detection does not know (e.g. university logos).
	 * More expensive than logo or text detection.
	 */
	List<String> detectWebEntities(byte[] image) throws Exception;

	/** OCR paragraphs with their bounding boxes from a single DOCUMENT_TEXT_DETECTION pass. */
	List<VisionTextBlock> detectTextBlocks(byte[] image) throws Exception;
}

package br.com.uhunter.validations.navigation.utils;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import br.com.uhunter.api.dto.NavigationValidationResultDto;
import br.com.uhunter.utils.ImageUtils;
import br.com.uhunter.page.PageDocument;
import br.com.uhunter.vision.VisionApi;
import br.com.uhunter.vision.VisionTextBlock;

/**
 * Left-side navigation detector using one Vision document pass + shared HTML.
 * Aligns with the thesis examples (Wikipedia / vertical course menus), not top horizontal nav.
 */
public final class NavigationOnLeftUtils {

	/** Reported for {@code hicksLaw} when there is nothing to judge (kept for API compatibility). */
	public static final String NOT_APPLICABLE = "not applicable";

	private NavigationOnLeftUtils() {
	}

	public static NavigationValidationResultDto analyze(VisionApi vision, byte[] byteImage, PageDocument page)
			throws Exception {
		BufferedImage bufferedImage = ImageUtils.byteArrayToBufferedImage(byteImage);
		List<VisionTextBlock> paragraphs = vision.detectTextBlocks(byteImage);
		List<VisionTextBlock> leftBlocks = NavigationHeuristicsUtils.leftRegionBlocks(
				paragraphs, bufferedImage.getWidth(), NavigationHeuristicsUtils.LEFT_EDGE_RATIO);
		List<String> landmarkTexts = page.navigationLandmarkTexts();

		Map<String, String> links = page.linksByText();
		List<String> matchedItems = NavigationHeuristicsUtils.matchLinkTextsOnLeft(leftBlocks, links.keySet());
		int quantity = matchedItems.size();

		boolean onLeft = NavigationHeuristicsUtils.isLeftNavigation(
				paragraphs,
				leftBlocks,
				matchedItems,
				landmarkTexts,
				bufferedImage.getHeight(),
				bufferedImage.getWidth());

		List<String> visionLabels = NavigationHeuristicsUtils.visionOnlyNavLabels(
				leftBlocks, bufferedImage.getWidth());
		boolean usedVisionOnly = onLeft && quantity < NavigationHeuristicsUtils.MIN_MATCHED_LINKS;

		double confidence;
		String evidence;
		if (onLeft) {
			if (usedVisionOnly) {
				confidence = Math.min(1.0, 0.50 + (visionLabels.size() * 0.07));
				evidence = "vision-only left rail (" + visionLabels.size() + " short labels"
						+ (landmarkTexts.isEmpty() ? ", no DOM landmarks" : "") + ")";
			} else {
				confidence = Math.min(1.0, 0.55 + (quantity * 0.07));
				evidence = quantity + " vertically stacked left-side links matched"
						+ (landmarkTexts.isEmpty() ? "" : " (landmarks=" + landmarkTexts.size() + ")");
			}
		} else {
			confidence = Math.min(0.45, Math.max(quantity, visionLabels.size()) * 0.05);
			evidence = "no vertical left sidebar detected (matchedLinks=" + quantity
					+ ", visionLabels=" + visionLabels.size()
					+ ", landmarks=" + landmarkTexts.size() + ")";
		}

		if (!onLeft) {
			return NavigationValidationResultDto.builder()
					.navigationOnLeftCorner(false)
					.hicksLaw(NOT_APPLICABLE)
					.confidence(confidence)
					.evidence(evidence)
					.build();
		}

		List<String> hicksItems = usedVisionOnly ? visionLabels : matchedItems;
		Object hicksLaw = hicksItems.size() >= NavigationHeuristicsUtils.MIN_ITEMS_FOR_HICKS
				? NavigationHeuristicsUtils.passesHicksAlphabeticalHeuristic(hicksItems)
				: NOT_APPLICABLE;
		return NavigationValidationResultDto.builder()
				.navigationOnLeftCorner(true)
				.hicksLaw(hicksLaw)
				.confidence(confidence)
				.evidence(evidence)
				.build();
	}
}

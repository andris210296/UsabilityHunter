package br.com.uhunter.validations.navigation.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import br.com.uhunter.page.PageDocument;
import br.com.uhunter.vision.VisionTextBlock;

/**
 * Pure helpers for left-navigation and Hick's-law style ordering checks.
 * Thesis target: a vertical left sidebar (Wikipedia / course pages), not a top horizontal menu.
 */
public final class NavigationHeuristicsUtils {

	/** Classic left sidebar occupies roughly the left third, not half the viewport. */
	public static final double LEFT_EDGE_RATIO = 0.34;
	public static final double LANDMARK_MATCH_RATIO_THRESHOLD = 0.45;
	public static final int MIN_LANDMARK_HITS = 3;
	public static final int MIN_MATCHED_LINKS = 3;
	public static final double ALPHABETICAL_BLOCK_RATIO_THRESHOLD = 0.50;
	public static final int MIN_ALPHABETICAL_BLOCK_SIZE = 3;
	public static final int MIN_ITEMS_FOR_HICKS = 5;
	/** Minimum vertical span (fraction of image height) for a sidebar-like stack. */
	public static final double MIN_VERTICAL_SPAN_RATIO = 0.18;
	/** Nav-like OCR labels are short; long lines are usually body copy. */
	public static final int MAX_NAV_LABEL_CHARS = 28;
	/** Vision-only rail blocks should stay narrow (icon + label column). */
	public static final double MAX_NAV_BLOCK_WIDTH_RATIO = 0.28;

	private NavigationHeuristicsUtils() {
	}

	public static List<VisionTextBlock> leftHalfBlocks(List<VisionTextBlock> blocks, int imageWidth) {
		return leftRegionBlocks(blocks, imageWidth, 0.50);
	}

	public static List<VisionTextBlock> leftRegionBlocks(
			List<VisionTextBlock> blocks, int imageWidth, double widthRatio) {
		double edge = imageWidth * widthRatio;
		List<VisionTextBlock> left = new ArrayList<>();
		for (VisionTextBlock block : blocks) {
			if (block.vertices() == null || block.vertices().size() < 2) {
				continue;
			}
			int rightEdge = block.vertices().get(1).getX();
			int leftEdge = block.vertices().get(0).getX();
			// Prefer blocks whose center sits in the left column.
			double centerX = (leftEdge + rightEdge) / 2.0;
			if (centerX <= edge) {
				left.add(block);
			}
		}
		return left;
	}

	public static List<String> matchLinkTextsOnLeft(
			List<VisionTextBlock> leftBlocks,
			Iterable<String> linkTexts) {
		List<String> matched = new ArrayList<>();
		for (String link : linkTexts) {
			if (link == null || link.isBlank()) {
				continue;
			}
			for (VisionTextBlock block : leftBlocks) {
				if (blockContains(block, link)) {
					matched.add(link);
					break;
				}
			}
		}
		return matched;
	}

	public static List<VisionTextBlock> blocksForMatchedLinks(
			List<VisionTextBlock> leftBlocks,
			List<String> matchedLinks) {
		List<VisionTextBlock> matchedBlocks = new ArrayList<>();
		for (String link : matchedLinks) {
			for (VisionTextBlock block : leftBlocks) {
				if (blockContains(block, link)) {
					matchedBlocks.add(block);
					break;
				}
			}
		}
		return matchedBlocks;
	}

	public static boolean isLeftNavigation(
			List<VisionTextBlock> allBlocks,
			List<VisionTextBlock> leftBlocks,
			List<String> matchedLinks,
			List<String> landmarkTexts) {
		return isLeftNavigation(allBlocks, leftBlocks, matchedLinks, landmarkTexts, 0);
	}

	public static boolean isLeftNavigation(
			List<VisionTextBlock> allBlocks,
			List<VisionTextBlock> leftBlocks,
			List<String> matchedLinks,
			List<String> landmarkTexts,
			int imageHeight) {
		return isLeftNavigation(allBlocks, leftBlocks, matchedLinks, landmarkTexts, imageHeight, 0);
	}

	public static boolean isLeftNavigation(
			List<VisionTextBlock> allBlocks,
			List<VisionTextBlock> leftBlocks,
			List<String> matchedLinks,
			List<String> landmarkTexts,
			int imageHeight,
			int imageWidth) {
		if (matchedLinks != null && matchedLinks.size() >= MIN_MATCHED_LINKS) {
			List<VisionTextBlock> matchedBlocks = blocksForMatchedLinks(leftBlocks, matchedLinks);
			if (looksLikeVerticalStack(matchedBlocks, imageHeight)) {
				// Vertical stack of HTML-matched left links is enough.
				// Do not divide by full-page OCR line count — that breaks on tall screenshots.
				if (!landmarkTexts.isEmpty()) {
					List<String> landmarkHits = matchLinkTextsOnLeft(leftBlocks, landmarkTexts);
					double ratio = (double) landmarkHits.size() / landmarkTexts.size();
					if (landmarkHits.size() >= MIN_LANDMARK_HITS && ratio >= LANDMARK_MATCH_RATIO_THRESHOLD) {
						return true;
					}
				}
				return true;
			}
		}

		// SPA / icon rails (YouTube): DOM often has no nav landmarks; use Vision alone.
		return isVisionOnlyLeftSidebar(leftBlocks, imageHeight, imageWidth);
	}

	/**
	 * Detect a left icon/text rail from OCR only (short, narrow, vertically stacked labels).
	 */
	public static boolean isVisionOnlyLeftSidebar(
			List<VisionTextBlock> leftBlocks,
			int imageHeight,
			int imageWidth) {
		List<VisionTextBlock> labelBlocks = navLikeLabelBlocks(leftBlocks, imageWidth);
		return looksLikeVerticalStack(labelBlocks, imageHeight);
	}

	/** Short left-column labels sorted top-to-bottom (for evidence / Hick's). */
	public static List<String> visionOnlyNavLabels(
			List<VisionTextBlock> leftBlocks,
			int imageWidth) {
		List<VisionTextBlock> labelBlocks = navLikeLabelBlocks(leftBlocks, imageWidth);
		labelBlocks.sort(Comparator.comparingInt(b -> b.vertices().get(0).getY()));
		List<String> labels = new ArrayList<>();
		for (VisionTextBlock block : labelBlocks) {
			String label = primaryNavLabel(block);
			if (label != null && !labels.contains(label)) {
				labels.add(label);
			}
		}
		return labels;
	}

	private static List<VisionTextBlock> navLikeLabelBlocks(
			List<VisionTextBlock> leftBlocks,
			int imageWidth) {
		List<VisionTextBlock> labels = new ArrayList<>();
		if (leftBlocks == null) {
			return labels;
		}
		for (VisionTextBlock block : leftBlocks) {
			if (block.vertices() == null || block.vertices().size() < 2) {
				continue;
			}
			String label = primaryNavLabel(block);
			if (label == null) {
				continue;
			}
			int width = Math.abs(block.vertices().get(1).getX() - block.vertices().get(0).getX());
			if (imageWidth > 0 && width > imageWidth * MAX_NAV_BLOCK_WIDTH_RATIO) {
				continue;
			}
			labels.add(block);
		}
		return labels;
	}

	private static String primaryNavLabel(VisionTextBlock block) {
		List<String> lines = block.lines();
		if (lines == null || lines.isEmpty()) {
			return null;
		}
		String best = null;
		for (String line : lines) {
			String cleaned = line == null ? "" : line.replaceAll("\\s+", " ").trim();
			if (cleaned.length() < 3 || cleaned.length() > MAX_NAV_LABEL_CHARS) {
				continue;
			}
			// Prefer single-token / short phrase labels over sentences.
			if (cleaned.split(" ").length > 4) {
				continue;
			}
			best = cleaned;
			break;
		}
		return best;
	}

	/**
	 * Sidebars stack links top-to-bottom. Horizontal header menus share roughly the same Y.
	 */
	public static boolean looksLikeVerticalStack(List<VisionTextBlock> matchedBlocks, int imageHeight) {
		if (matchedBlocks == null || matchedBlocks.size() < MIN_MATCHED_LINKS) {
			return false;
		}
		List<Integer> tops = new ArrayList<>();
		for (VisionTextBlock block : matchedBlocks) {
			if (block.vertices() == null || block.vertices().isEmpty()) {
				continue;
			}
			tops.add(block.vertices().get(0).getY());
		}
		if (tops.size() < MIN_MATCHED_LINKS) {
			return false;
		}
		tops.sort(Comparator.naturalOrder());
		int span = tops.get(tops.size() - 1) - tops.get(0);
		if (span <= 0) {
			return false;
		}
		int distinctRows = 1;
		int prev = tops.get(0);
		for (int i = 1; i < tops.size(); i++) {
			if (Math.abs(tops.get(i) - prev) >= 12) {
				distinctRows++;
				prev = tops.get(i);
			}
		}
		if (distinctRows < MIN_MATCHED_LINKS) {
			return false;
		}
		if (imageHeight > 0) {
			return ((double) span / imageHeight) >= MIN_VERTICAL_SPAN_RATIO;
		}
		return span >= 48;
	}

	/**
	 * Longest consecutive run that is alphabetically sorted (case-insensitive).
	 */
	public static int longestAlphabeticalRun(List<String> items) {
		if (items == null || items.isEmpty()) {
			return 0;
		}
		int best = 1;
		int current = 1;
		for (int i = 1; i < items.size(); i++) {
			if (items.get(i).compareToIgnoreCase(items.get(i - 1)) >= 0) {
				current++;
				best = Math.max(best, current);
			} else {
				current = 1;
			}
		}
		return best;
	}

	public static boolean passesHicksAlphabeticalHeuristic(List<String> itemsInVisualOrder) {
		if (itemsInVisualOrder == null || itemsInVisualOrder.size() < MIN_ITEMS_FOR_HICKS) {
			return false;
		}
		int run = longestAlphabeticalRun(itemsInVisualOrder);
		if (run < MIN_ALPHABETICAL_BLOCK_SIZE) {
			return false;
		}
		double ratio = (double) run / itemsInVisualOrder.size();
		return ratio >= ALPHABETICAL_BLOCK_RATIO_THRESHOLD;
	}

	private static boolean blockContains(VisionTextBlock block, String needle) {
		String normalizedNeedle = PageDocument.normalizeForCompare(needle);
		if (normalizedNeedle.length() < 3) {
			return false;
		}
		for (String line : block.lines()) {
			String normalizedLine = PageDocument.normalizeForCompare(line);
			if (normalizedLine.contains(normalizedNeedle) || normalizedNeedle.contains(normalizedLine)) {
				if (Math.min(normalizedLine.length(), normalizedNeedle.length()) >= 3) {
					return true;
				}
			}
		}
		String haystack = PageDocument.normalizeForCompare(block.text());
		return haystack.contains(normalizedNeedle);
	}
}

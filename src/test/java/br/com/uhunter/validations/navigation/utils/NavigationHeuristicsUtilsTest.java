package br.com.uhunter.validations.navigation.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.cloud.vision.v1.Vertex;

import br.com.uhunter.vision.VisionTextBlock;

class NavigationHeuristicsUtilsTest {

	@Test
	void leftHalfKeepsOnlyLeftBlocks() {
		VisionTextBlock left = block(10, 40, 10, "Home");
		VisionTextBlock right = block(300, 500, 10, "Ads");
		List<VisionTextBlock> filtered = NavigationHeuristicsUtils.leftHalfBlocks(List.of(left, right), 600);
		assertEquals(1, filtered.size());
		assertEquals("Home", filtered.get(0).text());
	}

	@Test
	void leftRegionUsesLeftThird() {
		VisionTextBlock farLeft = block(10, 40, 10, "Home");
		VisionTextBlock midPage = block(250, 310, 10, "About");
		List<VisionTextBlock> filtered = NavigationHeuristicsUtils.leftRegionBlocks(
				List.of(farLeft, midPage), 600, NavigationHeuristicsUtils.LEFT_EDGE_RATIO);
		assertEquals(1, filtered.size());
		assertEquals("Home", filtered.get(0).text());
	}

	@Test
	void matchesLinksPresentOnLeft() {
		VisionTextBlock left = block(10, 40, 10, "Home About Contact");
		List<String> matched = NavigationHeuristicsUtils.matchLinkTextsOnLeft(
				List.of(left), List.of("Home", "Pricing"));
		assertEquals(List.of("Home"), matched);
	}

	@Test
	void detectsLeftNavFromVerticalStack() {
		List<VisionTextBlock> blocks = List.of(
				block(10, 40, 20, "Home"),
				block(10, 40, 60, "About"),
				block(10, 40, 100, "Docs"),
				block(10, 40, 140, "Blog"),
				block(300, 500, 20, "Article body text"));
		List<VisionTextBlock> left = NavigationHeuristicsUtils.leftRegionBlocks(
				blocks, 600, NavigationHeuristicsUtils.LEFT_EDGE_RATIO);
		List<String> matched = List.of("Home", "About", "Docs", "Blog");
		assertTrue(NavigationHeuristicsUtils.isLeftNavigation(blocks, left, matched, List.of(), 400, 600));
	}

	@Test
	void matchedVerticalStackIgnoresDenseFullPageOcr() {
		List<VisionTextBlock> blocks = new java.util.ArrayList<>(List.of(
				block(10, 40, 20, "Home"),
				block(10, 40, 80, "About"),
				block(10, 40, 140, "Docs")));
		for (int i = 0; i < 40; i++) {
			blocks.add(block(250, 500, 20 + i * 12, "Video title number " + i));
		}
		List<VisionTextBlock> left = NavigationHeuristicsUtils.leftRegionBlocks(
				blocks, 600, NavigationHeuristicsUtils.LEFT_EDGE_RATIO);
		List<String> matched = List.of("Home", "About", "Docs");
		assertTrue(NavigationHeuristicsUtils.isLeftNavigation(blocks, left, matched, List.of(), 500, 600));
	}

	@Test
	void detectsVisionOnlyIconRailWithoutHtmlMatches() {
		List<VisionTextBlock> blocks = List.of(
				block(8, 70, 40, "Home"),
				block(8, 70, 100, "Shorts"),
				block(8, 70, 160, "Subscriptions"),
				block(8, 70, 220, "You"),
				block(200, 520, 40, "Recommended video title here"));
		List<VisionTextBlock> left = NavigationHeuristicsUtils.leftRegionBlocks(
				blocks, 600, NavigationHeuristicsUtils.LEFT_EDGE_RATIO);
		assertTrue(NavigationHeuristicsUtils.isLeftNavigation(
				blocks, left, List.of(), List.of(), 500, 600));
		assertEquals(List.of("Home", "Shorts", "Subscriptions", "You"),
				NavigationHeuristicsUtils.visionOnlyNavLabels(left, 600));
	}

	@Test
	void rejectsHorizontalHeaderStyleMatches() {
		// Same Y ≈ top menu bar (USP homepage style)
		List<VisionTextBlock> blocks = List.of(
				block(20, 80, 12, "Ensino"),
				block(120, 220, 12, "Pesquisa"),
				block(240, 360, 12, "Cultura"),
				block(20, 80, 12, "Comunicacao"));
		List<VisionTextBlock> left = NavigationHeuristicsUtils.leftRegionBlocks(
				blocks, 600, NavigationHeuristicsUtils.LEFT_EDGE_RATIO);
		List<String> matched = List.of("Ensino", "Pesquisa", "Cultura");
		assertFalse(NavigationHeuristicsUtils.isLeftNavigation(blocks, left, matched, List.of(), 400, 600));
	}

	@Test
	void hicksAlphabeticalPass() {
		List<String> items = List.of("Africa", "America", "Asia", "Europe", "Oceania");
		assertTrue(NavigationHeuristicsUtils.passesHicksAlphabeticalHeuristic(items));
		assertEquals(5, NavigationHeuristicsUtils.longestAlphabeticalRun(items));
	}

	@Test
	void hicksAlphabeticalFailWhenShuffled() {
		List<String> items = List.of("Europe", "Africa", "Oceania", "America", "Asia", "Antarctica");
		assertFalse(NavigationHeuristicsUtils.passesHicksAlphabeticalHeuristic(items));
	}

	private static VisionTextBlock block(int leftX, int rightX, int topY, String text) {
		List<Vertex> vertices = List.of(
				Vertex.newBuilder().setX(leftX).setY(topY).build(),
				Vertex.newBuilder().setX(rightX).setY(topY).build(),
				Vertex.newBuilder().setX(rightX).setY(topY + 20).build(),
				Vertex.newBuilder().setX(leftX).setY(topY + 20).build());
		return VisionTextBlock.builder()
				.vertices(vertices)
				.lines(List.of(text))
				.text(text)
				.build();
	}
}

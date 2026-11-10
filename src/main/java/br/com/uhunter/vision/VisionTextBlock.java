package br.com.uhunter.vision;

import java.util.List;

import com.google.cloud.vision.v1.Vertex;

import lombok.Builder;

/**
 * OCR paragraph extracted from a single DOCUMENT_TEXT_DETECTION pass.
 */
@Builder
public record VisionTextBlock(
		List<Vertex> vertices,
		List<String> lines,
		String text) {
}

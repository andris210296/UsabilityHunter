package br.com.uhunter.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

/**
 * Preserves the historical nested {@code performanceTest.performanceTest} JSON shape.
 */
@Builder
public record PerformanceValidationResultDto(
		@JsonProperty("performanceTest") Object performanceTest) {
}

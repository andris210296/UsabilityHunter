package br.com.uhunter.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public record UsabilityValidationResultDto(
		@JsonProperty("logoOnTopLeft") LogoValidationResultDto logoOnTopLeft,
		@JsonProperty("navigationOnLeftCornerTest") NavigationValidationResultDto navigationOnLeftCornerTest,
		@JsonProperty("isMobileFriendlyTest") ResponsiveValidationResultDto isMobileFriendlyTest,
		@JsonProperty("performanceTest") PerformanceValidationResultDto performanceTest) {
}

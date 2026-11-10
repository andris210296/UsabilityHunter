package br.com.uhunter.api.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.uhunter.validations.performance.PerformanceMetric.PerformanceMetricValues;
import lombok.Builder;

@Builder
public record PerformanceMetricDto(
		@JsonProperty("type") PerformanceMetricValues type,
		@JsonProperty("description") String description,
		@JsonProperty("pageContentLoadDistributionAverage") String pageContentLoadDistributionAverage,
		@JsonProperty("pageSpeed") Integer pageSpeed,
		@JsonProperty("pageContentLoadDistribution") Map<String, Double> pageContentLoadDistribution) {
}

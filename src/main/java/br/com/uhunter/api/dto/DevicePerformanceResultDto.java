package br.com.uhunter.api.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public record DevicePerformanceResultDto(
		@JsonProperty("optimizationPoints") int optimizationPoints,
		@JsonProperty("loadingOverall") String loadingOverall,
		@JsonProperty("loadingPerformance") Map<String, List<PerformanceMetricDto>> loadingPerformance) {
}

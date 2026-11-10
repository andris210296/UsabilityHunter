package br.com.uhunter.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public record PerformanceDevicesResultDto(
		@JsonProperty("mobileTest") DevicePerformanceResultDto mobileTest,
		@JsonProperty("desktopTest") DevicePerformanceResultDto desktopTest) {
}

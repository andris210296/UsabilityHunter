package br.com.uhunter.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NavigationValidationResultDto(
		@JsonProperty("navigationOnLeftCorner") Object navigationOnLeftCorner,
		@JsonProperty("hicksLaw") Object hicksLaw,
		@JsonProperty("confidence") Double confidence,
		@JsonProperty("evidence") String evidence) {

	public NavigationValidationResultDto(Object navigationOnLeftCorner, Object hicksLaw) {
		this(navigationOnLeftCorner, hicksLaw, null, null);
	}
}

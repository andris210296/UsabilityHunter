package br.com.uhunter.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LogoValidationResultDto(
		@JsonProperty("logoName") Object logoName,
		@JsonProperty("result") Object result,
		@JsonProperty("confidence") Double confidence,
		@JsonProperty("evidence") String evidence) {

	public LogoValidationResultDto(Object logoName, Object result) {
		this(logoName, result, null, null);
	}
}

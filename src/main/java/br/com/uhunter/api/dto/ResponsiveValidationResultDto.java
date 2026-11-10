package br.com.uhunter.api.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponsiveValidationResultDto(
		@JsonProperty("isMobileFriendly") Object isMobileFriendly,
		@JsonProperty("listOfIssues") Map<String, String> listOfIssues,
		@JsonProperty("confidence") Double confidence,
		@JsonProperty("evidence") String evidence) {

	public ResponsiveValidationResultDto(Object isMobileFriendly, Map<String, String> listOfIssues) {
		this(isMobileFriendly, listOfIssues, null, null);
	}
}

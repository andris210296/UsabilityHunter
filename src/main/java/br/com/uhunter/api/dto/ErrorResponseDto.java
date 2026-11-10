package br.com.uhunter.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public record ErrorResponseDto(@JsonProperty("error") String error) {
}

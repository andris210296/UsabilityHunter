package br.com.uhunter.service;

import br.com.uhunter.api.dto.UsabilityValidationResultDto;

public interface UsabilityValidationService {

	UsabilityValidationResultDto runValidations(String url) throws Exception;
}

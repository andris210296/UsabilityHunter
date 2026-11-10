package br.com.uhunter.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.uhunter.api.dto.UsabilityValidationResultDto;
import br.com.uhunter.service.UsabilityValidationService;

/**
 * Error mapping lives in {@link ApiExceptionHandler}; URL safety is enforced once, in the service.
 */
@RestController
public class UsabilityController {

	private final UsabilityValidationService usabilityValidationService;

	public UsabilityController(UsabilityValidationService usabilityValidationService) {
		this.usabilityValidationService = usabilityValidationService;
	}

	/**
	 * Runs the usability suite for a given page URL.
	 * {@code /uhunter/runtests} is the current path; the two {@code urest} paths are the original
	 * thesis endpoints, kept as aliases so existing clients keep working.
	 */
	@GetMapping(value = { "/uhunter/runtests", "/uhunter/urest/runtest", "/rest/urest/runtest" },
			produces = MediaType.APPLICATION_JSON_VALUE)
	public UsabilityValidationResultDto runTests(@RequestParam("url") String url) throws Exception {
		return usabilityValidationService.runValidations(url);
	}
}

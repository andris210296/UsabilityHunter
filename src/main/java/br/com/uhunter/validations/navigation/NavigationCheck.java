package br.com.uhunter.validations.navigation;

import org.springframework.stereotype.Component;

import br.com.uhunter.api.dto.NavigationValidationResultDto;
import br.com.uhunter.validations.AbstractUsabilityCheck;
import br.com.uhunter.validations.AnalysisContext;
import br.com.uhunter.validations.navigation.utils.NavigationOnLeftUtils;
import br.com.uhunter.vision.VisionApi;

/**
 * Is there a vertical navigation list on the left, and (if long enough) is it ordered (Hick's law)?
 */
@Component
public class NavigationCheck extends AbstractUsabilityCheck<NavigationValidationResultDto> {

	private final VisionApi vision;

	public NavigationCheck(VisionApi vision) {
		this.vision = vision;
	}

	@Override
	protected String name() {
		return "Navigation";
	}

	@Override
	protected NavigationValidationResultDto execute(AnalysisContext context) throws Exception {
		return NavigationOnLeftUtils.analyze(vision, context.screenshot().topLeft().bytes(), context.page());
	}

	@Override
	protected NavigationValidationResultDto onFailure(Exception e) {
		return NavigationValidationResultDto.builder()
				.navigationOnLeftCorner(messageOf(e))
				.hicksLaw(null)
				.confidence(0.0)
				.evidence("navigation check failed")
				.build();
	}
}

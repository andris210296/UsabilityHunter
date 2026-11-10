package br.com.uhunter.validations.responsive;

import org.springframework.stereotype.Component;

import br.com.uhunter.api.dto.ResponsiveValidationResultDto;
import br.com.uhunter.validations.AbstractUsabilityCheck;
import br.com.uhunter.validations.AnalysisContext;
import br.com.uhunter.validations.responsive.utils.IsMobileFriendlyUtils;

/**
 * Is the page mobile friendly? HTML heuristics refined by Lighthouse mobile audits.
 */
@Component
public class ResponsiveCheck extends AbstractUsabilityCheck<ResponsiveValidationResultDto> {

	@Override
	protected String name() {
		return "Responsive";
	}

	@Override
	protected ResponsiveValidationResultDto execute(AnalysisContext context) {
		return IsMobileFriendlyUtils.analyze(context.page(), context.pageSpeed());
	}

	@Override
	protected ResponsiveValidationResultDto onFailure(Exception e) {
		return ResponsiveValidationResultDto.builder()
				.isMobileFriendly(e.getMessage())
				.listOfIssues(null)
				.confidence(0.0)
				.evidence("responsive check failed")
				.build();
	}
}

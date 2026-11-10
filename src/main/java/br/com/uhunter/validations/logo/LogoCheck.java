package br.com.uhunter.validations.logo;

import org.springframework.stereotype.Component;

import br.com.uhunter.api.dto.LogoValidationResultDto;
import br.com.uhunter.screenshot.Screenshot;
import br.com.uhunter.validations.AbstractUsabilityCheck;
import br.com.uhunter.validations.AnalysisContext;
import br.com.uhunter.validations.logo.utils.LogoIdentificationUtils;
import br.com.uhunter.vision.VisionApi;

/**
 * Is the site's logo in the top-left corner? Tries the whole top-left region first, then its four
 * quarters, keeping the most confident match.
 */
@Component
public class LogoCheck extends AbstractUsabilityCheck<LogoValidationResultDto> {

	private final VisionApi vision;

	public LogoCheck(VisionApi vision) {
		this.vision = vision;
	}

	@Override
	protected String name() {
		return "Logo";
	}

	@Override
	protected LogoValidationResultDto execute(AnalysisContext context) throws Exception {
		Screenshot topLeft = context.screenshot().topLeft();
		LogoValidationResultDto match = LogoIdentificationUtils.findLogoInImage(vision, topLeft.bytes(), context.page());

		if (!LogoIdentificationUtils.isHit(match)) {
			for (Screenshot quadrant : topLeft.quadrants()) {
				LogoValidationResultDto candidate =
						LogoIdentificationUtils.findLogoInImage(vision, quadrant.bytes(), context.page());
				if (confidenceOf(candidate) > confidenceOf(match)) {
					match = candidate;
					if (LogoIdentificationUtils.isHit(candidate)) {
						break;
					}
				}
			}
		}

		if (LogoIdentificationUtils.isHit(match)) {
			return match;
		}
		return LogoValidationResultDto.builder()
				.logoName(null)
				.result(false)
				.confidence(match.confidence())
				.evidence(match.evidence() != null ? match.evidence() : "no brand match in top-left")
				.build();
	}

	@Override
	protected LogoValidationResultDto onFailure(Exception e) {
		return LogoValidationResultDto.builder()
				.logoName(e.getMessage())
				.result(false)
				.confidence(0.0)
				.evidence("logo check failed")
				.build();
	}

	private static double confidenceOf(LogoValidationResultDto result) {
		return result.confidence() == null ? 0.0 : result.confidence();
	}
}

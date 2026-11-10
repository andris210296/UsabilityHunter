package br.com.uhunter.validations.logo.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.uhunter.api.dto.LogoValidationResultDto;

class LogoIdentificationUtilsTest {

	@Test
	void acceptsCloseBrandMatch() {
		LogoValidationResultDto match = LogoIdentificationUtils.bestMatch(
				List.of("Wikipedia"), List.of("Wikipedia"));
		assertTrue(LogoIdentificationUtils.isHit(match));
		assertEquals("Wikipedia", match.logoName());
		assertTrue(match.confidence() >= LogoIdentificationUtils.MIN_ACCEPT_SCORE);
	}

	@Test
	void rejectsShortNoisyTokens() {
		LogoValidationResultDto match = LogoIdentificationUtils.bestMatch(
				List.of("a", "to", "of"), List.of("Wikipedia"));
		assertFalse(LogoIdentificationUtils.isHit(match));
	}

	@Test
	void matchesContainedBrandFragment() {
		LogoValidationResultDto match = LogoIdentificationUtils.bestMatch(
				List.of("Wikipedi"), List.of("Wikipedia"));
		assertTrue(match.confidence() >= 0.75);
	}

	@Test
	void similarityExactIsOne() {
		assertEquals(1.0, LogoIdentificationUtils.similarity("OpenAI", "OpenAI"));
	}

	@Test
	void rejectsOfSaoStyleGarbage() {
		assertFalse(LogoIdentificationUtils.isAcceptableToken("of São"));
		assertFalse(LogoIdentificationUtils.isAcceptableToken("de São"));
		LogoValidationResultDto match = LogoIdentificationUtils.bestMatch(
				List.of("of São", "de São"),
				List.of("USP – Universidade de São Paulo", "usp", "de São"),
				"usp");
		assertFalse(LogoIdentificationUtils.isHit(match));
	}

	@Test
	void prefersUspOverSaoPauloPlaceName() {
		LogoValidationResultDto match = LogoIdentificationUtils.bestMatch(
				List.of("USP", "São Paulo", "Universidade de São Paulo"),
				List.of("usp", "USP", "USP – Universidade de São Paulo"),
				"usp");
		assertTrue(LogoIdentificationUtils.isHit(match));
		assertEquals("USP", match.logoName());
		assertTrue(LogoIdentificationUtils.isStrongHit(match, "usp"));
	}

	@Test
	void rejectsSaoPauloAsBrandWhenOnlyPlaceNameMatches() {
		LogoValidationResultDto match = LogoIdentificationUtils.bestMatch(
				List.of("São Paulo"),
				List.of("USP – Universidade de São Paulo", "usp"),
				"usp");
		assertFalse(LogoIdentificationUtils.isHit(match),
				"city fragment alone must not count as the logo");
	}

	@Test
	void acceptsShortAcronymBrand() {
		LogoValidationResultDto match = LogoIdentificationUtils.bestMatch(
				List.of("USP"), List.of("usp", "Universidade de São Paulo"), "usp");
		assertTrue(LogoIdentificationUtils.isHit(match));
		assertEquals("USP", match.logoName());
	}

	@Test
	void weakLogoHitIsNotStrongEnoughToSkipWebDetection() {
		LogoValidationResultDto weak = LogoValidationResultDto.builder()
				.logoName("Something Long Enough")
				.result(true)
				.confidence(0.8)
				.evidence("logo-detection: matched")
				.build();
		assertTrue(LogoIdentificationUtils.isHit(weak));
		assertFalse(LogoIdentificationUtils.isStrongHit(weak, "usp"));
	}
}

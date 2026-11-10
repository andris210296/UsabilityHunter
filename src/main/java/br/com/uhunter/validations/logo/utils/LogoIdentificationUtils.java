package br.com.uhunter.validations.logo.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.text.similarity.LevenshteinDistance;

import br.com.uhunter.api.dto.LogoValidationResultDto;
import br.com.uhunter.page.PageDocument;
import br.com.uhunter.vision.VisionApi;
import lombok.Builder;

public final class LogoIdentificationUtils {

	/** Minimum similarity score (0–1) to accept a logo/text match. */
	public static final double MIN_ACCEPT_SCORE = 0.78;

	/** Prefer acronyms like "USP" but reject tiny stopword fragments like "São". */
	public static final int MIN_ACCEPT_TOKEN_LENGTH = 3;

	/** Only allow fuzzy Levenshtein boosts on longer strings (avoids "of São" ≈ "de São"). */
	private static final int MIN_LENGTH_FOR_FUZZY_BOOST = 8;
	private static final int MAX_ABSOLUTE_DISTANCE = 2;

	private static final Set<String> STOPWORDS = Set.of(
			"a", "o", "os", "as", "e", "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas",
			"um", "uma", "para", "por", "com", "the", "of", "and", "or", "to", "in", "on", "for",
			"sao", "paulo", "brasil", "brazil", "www", "http", "https", "org", "br");

	/** Place-name fragments that often appear next to logos but are not the brand. */
	private static final Set<String> WEAK_PLACE_PHRASES = Set.of(
			"saopaulo", "riodejaneiro", "belohorizonte", "portoalegre", "brasilia", "curitiba",
			"campinas", "santos", "newyork", "sanfrancisco", "losangeles", "desao", "ofsao",
			"desaopaulo", "ofsaopaulo");

	private LogoIdentificationUtils() {
	}

	public static LogoValidationResultDto findLogoInImage(VisionApi vision, byte[] byteImage, PageDocument page)
			throws Exception {
		return findLogoInImage(vision, byteImage, page.brandCandidates(), page.hostBrandHint());
	}

	public static LogoValidationResultDto findLogoInImage(
			VisionApi vision, byte[] byteImage, List<String> brandCandidates, String preferredBrand)
			throws Exception {
		// 1) Popular commercial logos — only accept strong hits so garbage does not skip web detection
		LogoValidationResultDto fromLogo = matchFromSource(
				vision.detectLogo(byteImage), brandCandidates, preferredBrand, "logo-detection");
		if (isStrongHit(fromLogo, preferredBrand)) {
			return fromLogo;
		}

		// 2) Web entities when logo DB misses (university / local marks)
		LogoValidationResultDto fromWeb = matchFromSource(
				vision.detectWebEntities(byteImage), brandCandidates, preferredBrand, "web-detection");
		if (isStrongHit(fromWeb, preferredBrand) || isHit(fromWeb)) {
			return fromWeb;
		}

		// 3) OCR fallback
		LogoValidationResultDto fromText = matchFromSource(
				vision.detectText(byteImage), brandCandidates, preferredBrand, "text-detection");
		if (isHit(fromText)) {
			return fromText;
		}

		return strongest(fromLogo, fromWeb, fromText);
	}

	private static LogoValidationResultDto matchFromSource(
			List<String> detections, List<String> brands, String preferredBrand, String source) {
		LogoValidationResultDto match = bestMatch(detections, brands, preferredBrand);
		if (match.evidence() == null || match.evidence().isBlank()) {
			return match;
		}
		return match.toBuilder()
				.evidence(source + ": " + match.evidence())
				.build();
	}

	private static LogoValidationResultDto strongest(LogoValidationResultDto... candidates) {
		LogoValidationResultDto best = candidates[0];
		for (LogoValidationResultDto candidate : candidates) {
			double bestScore = best.confidence() == null ? 0.0 : best.confidence();
			double score = candidate.confidence() == null ? 0.0 : candidate.confidence();
			if (score > bestScore) {
				best = candidate;
			}
		}
		return best;
	}

	public static boolean isHit(LogoValidationResultDto result) {
		return result != null && Boolean.TRUE.equals(result.result());
	}

	/**
	 * High-confidence brand mark: preferred host, acronym, institutional name, or near-exact match.
	 */
	public static boolean isStrongHit(LogoValidationResultDto result, String preferredBrand) {
		if (!isHit(result) || result.logoName() == null) {
			return false;
		}
		String name = String.valueOf(result.logoName());
		String preferredNorm = PageDocument.normalizeForCompare(preferredBrand);
		String nameNorm = PageDocument.normalizeForCompare(name);
		if (!preferredNorm.isEmpty() && nameNorm.equals(preferredNorm)) {
			return true;
		}
		if (isAcronymLike(name)) {
			return true;
		}
		if (looksInstitutional(name)) {
			return true;
		}
		return result.confidence() != null && result.confidence() >= 0.95 && contentCore(name).length() >= 5;
	}

	public static LogoValidationResultDto bestMatch(List<String> detectedStrings, List<String> brands) {
		return bestMatch(detectedStrings, brands, "");
	}

	public static LogoValidationResultDto bestMatch(
			List<String> detectedStrings, List<String> brands, String preferredBrand) {
		Candidate best = null;
		if (detectedStrings == null || detectedStrings.isEmpty() || brands == null || brands.isEmpty()) {
			return toResult(null, 0.0, "no detections");
		}

		String preferredNorm = PageDocument.normalizeForCompare(preferredBrand);

		for (String detected : detectedStrings) {
			for (String detectedCombo : meaningfulCombinations(detected)) {
				for (String brand : brands) {
					for (String brandCombo : meaningfulCombinations(brand)) {
						double score = similarity(detectedCombo, brandCombo);
						if (score < MIN_ACCEPT_SCORE) {
							continue;
						}
						if (!isAcceptableToken(detectedCombo) || !isAcceptableToken(brandCombo)) {
							continue;
						}
						Candidate candidate = Candidate.builder()
								.detected(detectedCombo)
								.brand(brandCombo)
								.score(score)
								.weight(rankWeight(detectedCombo, brandCombo, preferredNorm))
								.build();
						if (best == null || candidate.isBetterThan(best)) {
							best = candidate;
						}
					}
				}
			}
		}

		if (best == null) {
			return toResult(null, 0.0, "no strong brand match");
		}
		return toResult(
				best.detected,
				best.score,
				"matched '" + best.detected + "' to brand '" + best.brand + "'");
	}

	public static double similarity(String left, String right) {
		String a = PageDocument.normalizeForCompare(left);
		String b = PageDocument.normalizeForCompare(right);
		if (a.isEmpty() || b.isEmpty()) {
			return 0.0;
		}
		if (a.equals(b)) {
			return 1.0;
		}

		// Prefer comparing content cores so stopwords do not create false near-matches.
		String coreA = contentCore(left);
		String coreB = contentCore(right);
		if (!coreA.isEmpty() && coreA.equals(coreB)) {
			return 1.0;
		}

		if (a.length() >= MIN_LENGTH_FOR_FUZZY_BOOST && b.length() >= MIN_LENGTH_FOR_FUZZY_BOOST) {
			if (a.contains(b) || b.contains(a)) {
				int shorter = Math.min(a.length(), b.length());
				int longer = Math.max(a.length(), b.length());
				return Math.max(0.8, (double) shorter / longer);
			}
		}

		int distance = LevenshteinDistance.getDefaultInstance().apply(a, b);
		int maxLen = Math.max(a.length(), b.length());
		double ratio = 1.0 - ((double) distance / maxLen);

		if (Math.min(a.length(), b.length()) >= MIN_LENGTH_FOR_FUZZY_BOOST
				&& distance <= MAX_ABSOLUTE_DISTANCE) {
			return Math.max(ratio, 0.85);
		}
		if (Math.min(a.length(), b.length()) < MIN_LENGTH_FOR_FUZZY_BOOST) {
			// Short tokens must be essentially exact (acronyms / short brands).
			return ratio >= 0.95 ? ratio : 0.0;
		}
		return ratio;
	}

	public static List<String> wordCombinations(String string) {
		if (string == null || string.isBlank()) {
			return List.of();
		}
		String[] words = string.trim().split("\\s+");
		List<String> combinations = new ArrayList<>();
		for (int i = 0; i < words.length; i++) {
			StringBuilder builder = new StringBuilder(words[i]);
			combinations.add(builder.toString());
			for (int j = i + 1; j < words.length; j++) {
				builder.append(' ').append(words[j]);
				combinations.add(builder.toString());
				combinations.add(builder.toString().replace(" ", ""));
			}
		}
		return combinations;
	}

	public static boolean isAcceptableToken(String token) {
		String normalized = PageDocument.normalizeForCompare(token);
		if (normalized.length() < MIN_ACCEPT_TOKEN_LENGTH) {
			return false;
		}
		if (STOPWORDS.contains(normalized) || WEAK_PLACE_PHRASES.contains(normalized)) {
			return false;
		}
		String core = contentCore(token);
		if (core.length() < MIN_ACCEPT_TOKEN_LENGTH) {
			return false;
		}
		if (STOPWORDS.contains(core) || WEAK_PLACE_PHRASES.contains(core)) {
			return false;
		}
		return true;
	}

	/**
	 * Normalized content after removing stopwords/place fillers from a phrase.
	 * {@code "of São"} / {@code "de São"} → empty/weak; {@code "Universidade de São Paulo"} → {@code universidade}.
	 */
	public static String contentCore(String token) {
		if (token == null || token.isBlank()) {
			return "";
		}
		StringBuilder core = new StringBuilder();
		for (String word : token.trim().split("\\s+")) {
			String normalized = PageDocument.normalizeForCompare(word);
			if (normalized.isEmpty() || STOPWORDS.contains(normalized) || WEAK_PLACE_PHRASES.contains(normalized)) {
				continue;
			}
			core.append(normalized);
		}
		String joined = core.toString();
		if (WEAK_PLACE_PHRASES.contains(joined)) {
			return "";
		}
		return joined;
	}

	public static boolean isWeakPlacePhrase(String token) {
		String normalized = PageDocument.normalizeForCompare(token);
		return WEAK_PLACE_PHRASES.contains(normalized) || WEAK_PLACE_PHRASES.contains(contentCore(token));
	}

	/**
	 * Higher is better. Prefer host/acronym brands and institutional names over city fragments.
	 */
	static int rankWeight(String detected, String brand, String preferredNorm) {
		String detectedNorm = PageDocument.normalizeForCompare(detected);
		String brandNorm = PageDocument.normalizeForCompare(brand);
		int weight = 0;

		if (!preferredNorm.isEmpty()
				&& (detectedNorm.equals(preferredNorm)
						|| brandNorm.equals(preferredNorm)
						|| contentCore(detected).equals(preferredNorm))) {
			weight += 100;
		}
		if (isAcronymLike(detected) || isAcronymLike(brand)) {
			weight += 40;
		}
		if (looksInstitutional(detected) || looksInstitutional(brand)) {
			weight += 30;
		}
		if (detectedNorm.length() <= 6 && contentCore(detected).length() >= 3) {
			weight += 10;
		}
		weight += Math.min(contentCore(detected).length(), 24);
		return weight;
	}

	private static boolean isAcronymLike(String token) {
		String trimmed = token == null ? "" : token.trim();
		if (trimmed.length() < 2 || trimmed.length() > 6) {
			return false;
		}
		String letters = trimmed.replaceAll("[^A-Za-z]", "");
		if (letters.length() < 2) {
			return false;
		}
		return letters.equals(letters.toUpperCase(Locale.ROOT));
	}

	private static boolean looksInstitutional(String token) {
		String normalized = PageDocument.normalizeForCompare(token);
		return normalized.contains("universidade")
				|| normalized.contains("university")
				|| normalized.contains("college")
				|| normalized.contains("institute")
				|| normalized.contains("faculdade");
	}

	private static List<String> meaningfulCombinations(String string) {
		List<String> out = new ArrayList<>();
		for (String combo : wordCombinations(string)) {
			if (isAcceptableToken(combo)) {
				out.add(combo);
			}
		}
		return out;
	}

	private static LogoValidationResultDto toResult(String logoName, double confidence, String evidence) {
		boolean hit = logoName != null && confidence >= MIN_ACCEPT_SCORE && isAcceptableToken(logoName);
		return LogoValidationResultDto.builder()
				.logoName(logoName)
				.result(hit)
				.confidence(confidence)
				.evidence(evidence)
				.build();
	}

	@Builder
	private record Candidate(String detected, String brand, double score, int weight) {
		boolean isBetterThan(Candidate other) {
			if (score > other.score + 0.02) {
				return true;
			}
			if (other.score > score + 0.02) {
				return false;
			}
			if (weight != other.weight) {
				return weight > other.weight;
			}
			return contentCore(detected).length() > contentCore(other.detected).length();
		}
	}
}

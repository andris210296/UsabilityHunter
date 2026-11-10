package br.com.uhunter.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import br.com.uhunter.api.dto.LogoValidationResultDto;
import br.com.uhunter.api.dto.NavigationValidationResultDto;
import br.com.uhunter.api.dto.PerformanceValidationResultDto;
import br.com.uhunter.api.dto.ResponsiveValidationResultDto;
import br.com.uhunter.api.dto.UsabilityValidationResultDto;
import br.com.uhunter.config.AsyncConfig;
import br.com.uhunter.config.UhunterProperties;
import br.com.uhunter.security.UrlSafetyValidator;
import br.com.uhunter.service.UsabilityValidationService;
import br.com.uhunter.validations.AnalysisContext;
import br.com.uhunter.validations.AnalysisContextFactory;
import br.com.uhunter.validations.UsabilityCheck;

@Service
public class UsabilityValidationServiceImpl implements UsabilityValidationService {

	private static final Logger log = LoggerFactory.getLogger(UsabilityValidationServiceImpl.class);

	private final UhunterProperties properties;
	private final AnalysisContextFactory contextFactory;
	private final UsabilityCheck<LogoValidationResultDto> logoCheck;
	private final UsabilityCheck<NavigationValidationResultDto> navigationCheck;
	private final UsabilityCheck<ResponsiveValidationResultDto> responsiveCheck;
	private final UsabilityCheck<PerformanceValidationResultDto> performanceCheck;
	private final Executor usabilityTaskExecutor;

	public UsabilityValidationServiceImpl(
			UhunterProperties properties,
			AnalysisContextFactory contextFactory,
			UsabilityCheck<LogoValidationResultDto> logoCheck,
			UsabilityCheck<NavigationValidationResultDto> navigationCheck,
			UsabilityCheck<ResponsiveValidationResultDto> responsiveCheck,
			UsabilityCheck<PerformanceValidationResultDto> performanceCheck,
			@Qualifier(AsyncConfig.USABILITY_TASK_EXECUTOR) Executor usabilityTaskExecutor) {
		this.properties = properties;
		this.contextFactory = contextFactory;
		this.logoCheck = logoCheck;
		this.navigationCheck = navigationCheck;
		this.responsiveCheck = responsiveCheck;
		this.performanceCheck = performanceCheck;
		this.usabilityTaskExecutor = usabilityTaskExecutor;
	}

	@Override
	public UsabilityValidationResultDto runValidations(String url) throws Exception {
		String safeUrl = UrlSafetyValidator.requireSafeHttpUrl(url).toString();
		long started = System.currentTimeMillis();
		log.info("runtest start url={}", safeUrl);

		AnalysisContext context = contextFactory.create(safeUrl);
		UsabilityValidationResultDto result = runChecks(context);

		log.info("runtest done url={} in {} ms", safeUrl, System.currentTimeMillis() - started);
		return result;
	}

	/**
	 * Runs the four heuristic checks in parallel on the shared executor.
	 *
	 * @throws RejectedExecutionException when the executor is saturated (already-submitted checks are cancelled)
	 * @throws TimeoutException           when the checks exceed {@code uhunter.validation-timeout}
	 */
	UsabilityValidationResultDto runChecks(AnalysisContext context) throws TimeoutException {
		List<CompletableFuture<?>> submitted = new ArrayList<>();
		CompletableFuture<LogoValidationResultDto> logo;
		CompletableFuture<NavigationValidationResultDto> navigation;
		CompletableFuture<ResponsiveValidationResultDto> responsive;
		CompletableFuture<PerformanceValidationResultDto> performance;
		try {
			logo = submit(submitted, logoCheck, context);
			navigation = submit(submitted, navigationCheck, context);
			responsive = submit(submitted, responsiveCheck, context);
			performance = submit(submitted, performanceCheck, context);
		} catch (RejectedExecutionException e) {
			log.warn("runtest rejected url={}: executor saturated", context.url());
			cancelAll(submitted);
			throw e;
		}

		Duration timeout = properties.getValidationTimeout();
		try {
			CompletableFuture.allOf(logo, navigation, responsive, performance)
					.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
					.join();
		} catch (CompletionException e) {
			cancelAll(submitted);
			if (e.getCause() instanceof TimeoutException) {
				log.warn("runtest timed out url={} after {} ms", context.url(), timeout.toMillis());
				throw new TimeoutException("Usability checks did not finish within " + timeout);
			}
			throw e;
		}

		return UsabilityValidationResultDto.builder()
				.logoOnTopLeft(logo.join())
				.navigationOnLeftCornerTest(navigation.join())
				.isMobileFriendlyTest(responsive.join())
				.performanceTest(performance.join())
				.build();
	}

	private <R> CompletableFuture<R> submit(
			List<CompletableFuture<?>> submitted, UsabilityCheck<R> check, AnalysisContext context) {
		CompletableFuture<R> future = CompletableFuture.supplyAsync(() -> check.run(context), usabilityTaskExecutor);
		submitted.add(future);
		return future;
	}

	/** Note: cancelling a {@link CompletableFuture} does not interrupt a check that is already running. */
	private static void cancelAll(List<CompletableFuture<?>> futures) {
		futures.forEach(f -> f.cancel(true));
	}
}

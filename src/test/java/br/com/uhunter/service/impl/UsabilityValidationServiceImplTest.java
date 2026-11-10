package br.com.uhunter.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.uhunter.api.dto.LogoValidationResultDto;
import br.com.uhunter.api.dto.NavigationValidationResultDto;
import br.com.uhunter.api.dto.PerformanceValidationResultDto;
import br.com.uhunter.api.dto.ResponsiveValidationResultDto;
import br.com.uhunter.api.dto.UsabilityValidationResultDto;
import br.com.uhunter.config.UhunterProperties;
import br.com.uhunter.page.PageDocument;
import br.com.uhunter.validations.AnalysisContext;
import br.com.uhunter.validations.UsabilityCheck;

class UsabilityValidationServiceImplTest {

	private static final String URL = "https://www.wikipedia.org";

	private final ExecutorService pool = Executors.newFixedThreadPool(4);
	private final AnalysisContext context = new AnalysisContext(
			URL,
			PageDocument.fromHtml(URL, "<html><head><title>Wikipedia</title></head></html>"),
			null,
			null);

	@AfterEach
	void tearDown() {
		pool.shutdownNow();
	}

	private static UsabilityCheck<LogoValidationResultDto> logoOk(AtomicInteger calls) {
		return ctx -> {
			calls.incrementAndGet();
			return LogoValidationResultDto.builder()
					.logoName("Wikipedia").result(true).confidence(0.95).evidence("matched").build();
		};
	}

	private static UsabilityCheck<NavigationValidationResultDto> navigationOk() {
		return ctx -> NavigationValidationResultDto.builder()
				.navigationOnLeftCorner(true).hicksLaw("not applicable").confidence(0.8).evidence("3 links").build();
	}

	private static UsabilityCheck<ResponsiveValidationResultDto> responsiveOk() {
		return ctx -> ResponsiveValidationResultDto.builder()
				.isMobileFriendly(true).listOfIssues(null).confidence(0.85).evidence("viewport ok").build();
	}

	private static UsabilityCheck<PerformanceValidationResultDto> performanceOk() {
		return ctx -> PerformanceValidationResultDto.builder().performanceTest("ok").build();
	}

	private static UhunterProperties timeout(Duration duration) {
		UhunterProperties properties = new UhunterProperties();
		properties.setValidationTimeout(duration);
		return properties;
	}

	private UsabilityValidationServiceImpl service(
			UhunterProperties properties,
			UsabilityCheck<LogoValidationResultDto> logo,
			UsabilityCheck<PerformanceValidationResultDto> performance,
			Executor executor) {
		return new UsabilityValidationServiceImpl(
				properties, null, logo, navigationOk(), responsiveOk(), performance, executor);
	}

	@Test
	void aggregatesResultsFromHeuristicChecks() throws Exception {
		UsabilityValidationServiceImpl service =
				service(timeout(Duration.ofSeconds(5)), logoOk(new AtomicInteger()), performanceOk(), pool);

		UsabilityValidationResultDto result = service.runChecks(context);

		assertNotNull(result);
		assertEquals(true, result.logoOnTopLeft().result());
		assertEquals("Wikipedia", result.logoOnTopLeft().logoName());
		assertEquals(0.95, result.logoOnTopLeft().confidence());
		assertEquals(true, result.navigationOnLeftCornerTest().navigationOnLeftCorner());
		assertEquals(true, result.isMobileFriendlyTest().isMobileFriendly());
		assertEquals("ok", result.performanceTest().performanceTest());
	}

	@Test
	void everyCheckReceivesTheSameSharedContext() throws Exception {
		AtomicReference<AnalysisContext> seenByLogo = new AtomicReference<>();
		AtomicReference<AnalysisContext> seenByPerformance = new AtomicReference<>();
		UsabilityValidationServiceImpl service = service(timeout(Duration.ofSeconds(5)),
				ctx -> {
					seenByLogo.set(ctx);
					return LogoValidationResultDto.builder().result(true).build();
				},
				ctx -> {
					seenByPerformance.set(ctx);
					return PerformanceValidationResultDto.builder().performanceTest("ok").build();
				},
				pool);

		service.runChecks(context);

		assertSame(context, seenByLogo.get(), "page and screenshot are built once and shared, not rebuilt per check");
		assertSame(context, seenByPerformance.get());
	}

	@Test
	void throwsTimeoutWhenACheckHangsPastTheConfiguredLimit() {
		CountDownLatch release = new CountDownLatch(1);
		UsabilityCheck<PerformanceValidationResultDto> hanging = ctx -> {
			try {
				release.await(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return PerformanceValidationResultDto.builder().performanceTest("late").build();
		};
		UsabilityValidationServiceImpl service =
				service(timeout(Duration.ofMillis(150)), logoOk(new AtomicInteger()), hanging, pool);

		long started = System.nanoTime();
		TimeoutException ex = assertThrows(TimeoutException.class, () -> service.runChecks(context));
		long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
		release.countDown();

		assertTrue(ex.getMessage().contains("PT0.15S"), ex.getMessage());
		assertTrue(elapsedMs < 5_000, "should fail fast, took " + elapsedMs + " ms");
	}

	@Test
	void rejectedExecutionCancelsChecksThatWereAlreadyQueued() {
		List<Runnable> queued = new ArrayList<>();
		AtomicInteger logoCalls = new AtomicInteger();
		// Accepts two tasks (they stay queued), then reports saturation like a full ThreadPoolTaskExecutor.
		Executor saturating = task -> {
			if (queued.size() >= 2) {
				throw new RejectedExecutionException("queue full");
			}
			queued.add(task);
		};
		UsabilityValidationServiceImpl service =
				service(timeout(Duration.ofSeconds(5)), logoOk(logoCalls), performanceOk(), saturating);

		assertThrows(RejectedExecutionException.class, () -> service.runChecks(context));

		// The queued tasks must now be no-ops, otherwise a rejected request would still burn API quota.
		queued.forEach(Runnable::run);
		assertEquals(0, logoCalls.get(), "cancelled logo check must not run");
	}
}

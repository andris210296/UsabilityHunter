package br.com.uhunter.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.uhunter.api.dto.LogoValidationResultDto;
import br.com.uhunter.api.dto.NavigationValidationResultDto;
import br.com.uhunter.api.dto.PerformanceValidationResultDto;
import br.com.uhunter.api.dto.ResponsiveValidationResultDto;
import br.com.uhunter.api.dto.UsabilityValidationResultDto;
import br.com.uhunter.security.UnsafeUrlException;
import br.com.uhunter.security.UrlSafetyValidator;
import br.com.uhunter.service.UsabilityValidationService;

class UsabilityControllerTest {

	private static final UsabilityValidationResultDto OK_RESULT = UsabilityValidationResultDto.builder()
			.logoOnTopLeft(LogoValidationResultDto.builder().logoName("Wikipedia").result(true).build())
			.navigationOnLeftCornerTest(NavigationValidationResultDto.builder()
					.navigationOnLeftCorner(true)
					.hicksLaw("not applicable")
					.build())
			.isMobileFriendlyTest(ResponsiveValidationResultDto.builder()
					.isMobileFriendly(true)
					.listOfIssues(null)
					.build())
			.performanceTest(PerformanceValidationResultDto.builder().performanceTest("ok").build())
			.build();

	private MockMvc mockMvcFor(UsabilityValidationService service) {
		return MockMvcBuilders.standaloneSetup(new UsabilityController(service))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		// Mirrors the real service: URL safety is enforced inside it, not in the controller.
		mockMvc = mockMvcFor(url -> {
			UrlSafetyValidator.requireSafeHttpUrl(url);
			return OK_RESULT;
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "/uhunter/runtests", "/uhunter/urest/runtest", "/rest/urest/runtest" })
	void runTestReturnsAggregatedPayloadOnCurrentAndLegacyPaths(String path) throws Exception {
		mockMvc.perform(get(path).param("url", "https://8.8.8.8/"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.logoOnTopLeft.result").value(true))
				.andExpect(jsonPath("$.logoOnTopLeft.logoName").value("Wikipedia"))
				.andExpect(jsonPath("$.navigationOnLeftCornerTest.navigationOnLeftCorner").value(true))
				.andExpect(jsonPath("$.isMobileFriendlyTest.isMobileFriendly").value(true));
	}

	@Test
	void runTestRejectsBlankUrl() throws Exception {
		mockMvc.perform(get("/uhunter/runtests").param("url", " "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void runTestRejectsMissingUrlWithJsonError() throws Exception {
		mockMvc.perform(get("/uhunter/runtests"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Query parameter 'url' is required"));
	}

	@Test
	void runTestRejectsLocalhostSsrf() throws Exception {
		mockMvc.perform(get("/uhunter/runtests").param("url", "http://127.0.0.1:8080/"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void saturatedExecutorReturns503WithRetryAfter() throws Exception {
		MockMvc busy = mockMvcFor(url -> {
			throw new RejectedExecutionException("queue full");
		});
		busy.perform(get("/uhunter/runtests").param("url", "https://8.8.8.8/"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().string("Retry-After", ApiExceptionHandler.RETRY_AFTER_SECONDS))
				.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void timeoutReturns504() throws Exception {
		MockMvc slow = mockMvcFor(url -> {
			throw new TimeoutException("Usability checks did not finish within PT4M");
		});
		slow.perform(get("/uhunter/runtests").param("url", "https://8.8.8.8/"))
				.andExpect(status().isGatewayTimeout())
				.andExpect(jsonPath("$.error").value("Usability checks did not finish within PT4M"));
	}

	@Test
	void wrappedAsyncFailureIsUnwrapped() throws Exception {
		MockMvc wrappedBusy = mockMvcFor(url -> {
			throw new CompletionException(new RejectedExecutionException("queue full"));
		});
		wrappedBusy.perform(get("/uhunter/runtests").param("url", "https://8.8.8.8/"))
				.andExpect(status().isServiceUnavailable());

		MockMvc wrappedUnsafe = mockMvcFor(url -> {
			throw new CompletionException(new UnsafeUrlException("nope"));
		});
		wrappedUnsafe.perform(get("/uhunter/runtests").param("url", "https://8.8.8.8/"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("nope"));
	}

	@Test
	void unexpectedFailureReturns500() throws Exception {
		MockMvc broken = mockMvcFor(url -> {
			throw new IllegalStateException("boom");
		});
		broken.perform(get("/uhunter/runtests").param("url", "https://8.8.8.8/"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error").value("boom"));
	}

	@Test
	void unrelatedIllegalArgumentIsNotReportedAsBadRequest() throws Exception {
		MockMvc broken = mockMvcFor(url -> {
			throw new IllegalArgumentException("Unknown screenshot provider 'x'");
		});
		broken.perform(get("/uhunter/runtests").param("url", "https://8.8.8.8/"))
				.andExpect(status().isInternalServerError());
	}

	@Test
	void unsupportedMethodKeepsSpringStatus() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.post("/uhunter/runtests").param("url", "https://8.8.8.8/"))
				.andExpect(status().isMethodNotAllowed());
	}
}

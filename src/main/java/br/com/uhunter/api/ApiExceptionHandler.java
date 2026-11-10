package br.com.uhunter.api;

import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.uhunter.api.dto.ErrorResponseDto;
import br.com.uhunter.security.UnsafeUrlException;

/**
 * Single place that maps failures to HTTP status codes and the {@link ErrorResponseDto} body.
 * Extends {@link ResponseEntityExceptionHandler} so Spring MVC's own errors (405, 415, ...) keep their
 * proper status instead of falling into the generic 500 handler.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	/** Seconds a client should wait before retrying when the executor is saturated. */
	static final String RETRY_AFTER_SECONDS = "10";

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(
			MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status,
			WebRequest request) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(error("Query parameter '" + ex.getParameterName() + "' is required"));
	}

	@ExceptionHandler(UnsafeUrlException.class)
	public ResponseEntity<ErrorResponseDto> handleUnsafeUrl(UnsafeUrlException e) {
		return ResponseEntity.badRequest().body(error(e.getMessage()));
	}

	/** Executor queue full: the service is healthy but busy, so tell the client to retry. */
	@ExceptionHandler(RejectedExecutionException.class)
	public ResponseEntity<ErrorResponseDto> handleBusy(RejectedExecutionException e) {
		log.warn("Rejecting request, usability executor saturated: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
				.body(error("Server is busy, please retry shortly"));
	}

	@ExceptionHandler(TimeoutException.class)
	public ResponseEntity<ErrorResponseDto> handleTimeout(TimeoutException e) {
		return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(error(e.getMessage()));
	}

	/** Async failures arrive wrapped; unwrap and route to the matching handler above. */
	@ExceptionHandler(CompletionException.class)
	public ResponseEntity<ErrorResponseDto> handleCompletion(CompletionException e) {
		return handleUnexpected(e.getCause() != null ? e.getCause() : e);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDto> handleAny(Exception e) {
		return handleUnexpected(e);
	}

	private ResponseEntity<ErrorResponseDto> handleUnexpected(Throwable t) {
		if (t instanceof UnsafeUrlException unsafe) {
			return handleUnsafeUrl(unsafe);
		}
		if (t instanceof RejectedExecutionException rejected) {
			return handleBusy(rejected);
		}
		if (t instanceof TimeoutException timeout) {
			return handleTimeout(timeout);
		}
		log.error("Request failed: {}", t.getMessage(), t);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(error(t.getMessage() != null ? t.getMessage() : t.toString()));
	}

	private static ErrorResponseDto error(String message) {
		return ErrorResponseDto.builder().error(message).build();
	}
}

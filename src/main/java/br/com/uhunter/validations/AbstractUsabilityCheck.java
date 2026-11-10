package br.com.uhunter.validations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template method: runs {@link #execute} and, if it fails, logs and returns {@link #onFailure}. One failing
 * heuristic therefore never discards the results of the other three.
 */
public abstract class AbstractUsabilityCheck<R> implements UsabilityCheck<R> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public final R run(AnalysisContext context) {
		try {
			return execute(context);
		} catch (Exception e) {
			log.error("{} check failed for url={}: {}", name(), context.url(), e.getMessage(), e);
			return onFailure(e);
		}
	}

	/** Short name for log messages, e.g. {@code "Logo"}. */
	protected abstract String name();

	protected abstract R execute(AnalysisContext context) throws Exception;

	/** Result reported to the client when {@link #execute} throws. */
	protected abstract R onFailure(Exception e);

	protected static String messageOf(Exception e) {
		return e.getMessage() != null ? e.getMessage() : e.toString();
	}
}

package br.com.uhunter.validations;

/**
 * One usability heuristic. All checks share this single signature; the orchestrator runs them in parallel.
 *
 * @param <R> the check's result DTO
 */
@FunctionalInterface
public interface UsabilityCheck<R> {

	/**
	 * Runs the heuristic. Implementations must not throw for expected failures; they report them in the
	 * returned result instead (see {@link AbstractUsabilityCheck}).
	 */
	R run(AnalysisContext context);
}

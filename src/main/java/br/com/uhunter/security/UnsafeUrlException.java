package br.com.uhunter.security;

/**
 * Thrown when a user-supplied URL must not be fetched by the server (bad input or SSRF target).
 * Kept distinct from {@link IllegalArgumentException} so unrelated programming errors are not
 * reported to clients as "bad request".
 */
public class UnsafeUrlException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;

	public UnsafeUrlException(String message) {
		super(message);
	}
}

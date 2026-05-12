package com.bloomscorp.nverse.support;

/**
 * Centralized human-readable message constants for NVerse authentication and security failures.
 *
 * <p>This class is non-instantiable. All constants are written to HTTP responses by
 * {@link com.bloomscorp.nverse.NVerseExceptionHandlerFilter} when the corresponding
 * exception is caught during filter-chain processing. Using these constants ensures
 * wording consistency across all services that depend on this library.</p>
 */
public final class Message {

	private Message() {}

	/** Response message when an I/O exception is raised during filter or servlet processing. */
	public static final String IO_FAILURE = "An I/O failure was encountered.";

	/** Response message when a {@link jakarta.servlet.ServletException} propagates through the filter chain. */
	public static final String SERVLET_FAILURE = "A servlet failed while trying to process your request.";

	/** Response message when a JWT token has passed its expiry time. */
	public static final String EXPIRED_CREDENTIALS = "The credentials used have expired.";

	/** Response message when an {@link IllegalArgumentException} is thrown during authentication, indicating structurally invalid credentials. */
	public static final String ILLEGAL_ARGUMENTS = "Illegal credentials were used to authenticate.";

	/** Response message when Spring Security rejects the supplied username/password pair. */
	public static final String BAD_CREDENTIALS = "The credentials provided were incorrect.";

	/** Response message when the account exists but has been disabled (inactive). */
	public static final String ACCOUNT_DISABLED = "The associated account is disabled and not accessible.";

	/** Response message when an internal authentication service error prevents credential verification. */
	public static final String INTERNAL_AUTHENTICATION_SERVICE_FAILURE = "Our system failed to authenticate the credentials.";

	/** Response message when the JWT token is structurally malformed or has been tampered with. */
	public static final String MALFORMED_CREDENTIALS = "The credentials have been tampered with.";

	/** Response message when no tenant account matches the supplied username (encrypted email). */
	public static final String USERNAME_NOT_FOUND = "The username does not have any associated account.";

	/** Response message when the account exists but is currently locked. */
	public static final String USER_ACCOUNT_LOCKED = "The user account may be locked. Contact systems administrator.";
}

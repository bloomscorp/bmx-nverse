package com.bloomscorp.nverse;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Validates email addresses using a compiled regular expression before they are
 * passed to {@link NVerseEmailEncoder} for encryption.
 *
 * <p>The regular expression enforces a basic RFC-compatible email format: at least
 * one local-part character, an {@code @} separator, at least one domain label, and a
 * top-level domain of two or more characters. Matching is case-insensitive.</p>
 *
 * <p>This class intentionally performs lightweight syntactic validation only; it does
 * not perform DNS lookups or SMTP verification.</p>
 */
public class NVerseEmailValidator {

	private static final String ESAPI_VALIDATION_CONTEXT_EMAIL = "EMAIL";
	private static final String ESAPI_VALIDATION_EXPRESSION_TYPE_EMAIL = "Email";
	private static final int EMAIL_MAX_LENGTH = 100;
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$", Pattern.CASE_INSENSITIVE);

	/**
	 * Returns the email address unchanged if it passes format validation, or {@code null}
	 * if the email is syntactically invalid or if any exception occurs during matching.
	 *
	 * @param email the email address to validate; must not be {@code null}.
	 * @return the original {@code email} string if valid; {@code null} otherwise.
	 */
	public String getValidatedEmail(@NotNull String email) {
		try {

			return EMAIL_PATTERN.matcher(email).matches() ? email : null;

			// TODO: bring back ESAPI
//			return NVerseESAPI.validator().getValidInput(
//				ESAPI_VALIDATION_CONTEXT_EMAIL,
//				email.toLowerCase(),
//				ESAPI_VALIDATION_EXPRESSION_TYPE_EMAIL,
//				EMAIL_MAX_LENGTH,
//				false
//			);
		} catch (Exception e) {
			return null;
		}
	}
}

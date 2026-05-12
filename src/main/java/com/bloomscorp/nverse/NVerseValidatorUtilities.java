package com.bloomscorp.nverse;

/**
 * Static utility class providing primitive validation helpers used across the NVerse
 * security and integrity check layers.
 */
public class NVerseValidatorUtilities {

	/**
	 * Returns {@code true} if the given numeric ID represents a valid, persisted entity.
	 *
	 * <p>In the NVerse model, auto-generated database primary keys are always positive.
	 * A value of {@code 0} or less indicates an uninitialized or invalid ID.</p>
	 *
	 * @param numericID the long primary key to validate.
	 * @return {@code true} if {@code numericID > 0}; {@code false} otherwise.
	 */
	public static boolean isGenericNumericIDValid(long numericID) {
		return numericID > 0;
	}
}

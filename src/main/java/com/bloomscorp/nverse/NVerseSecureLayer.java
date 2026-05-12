package com.bloomscorp.nverse;

import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import com.bloomscorp.pastebox.Pastebox;
import lombok.AllArgsConstructor;

/**
 * Performs the initial embedded integrity check on an authenticated tenant object,
 * acting as the first gate in the {@link NVerseGatekeeper} surveillance pipeline.
 *
 * <p>The check validates that the tenant object is not {@code null}, has a database
 * primary key greater than zero, and has a non-blank universally unique identifier (UID).
 * These properties collectively confirm that the object represents a legitimately
 * persisted entity rather than a forged or partially constructed one.</p>
 *
 * @param <T> the concrete tenant type, must implement {@link NVerseTenant}.
 * @param <E> the role enum type.
 * @param <R> the concrete role entity type, must implement {@link NVerseRole}.
 *
 * @see NVerseGatekeeper
 */
@AllArgsConstructor
public class NVerseSecureLayer<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	/**
	 * Returns {@code true} if the given tenant passes all initial embedded integrity checks.
	 *
	 * <p>The following conditions must all be satisfied:</p>
	 * <ul>
	 *   <li>{@code user} is not {@code null}.</li>
	 *   <li>{@link NVerseTenant#getId()} is greater than {@code 0}
	 *       (via {@link NVerseValidatorUtilities#isGenericNumericIDValid}).</li>
	 *   <li>{@link NVerseTenant#getUid()} is not {@code null} and not blank
	 *       (via {@link Pastebox#isEmptyString}).</li>
	 * </ul>
	 *
	 * @param user the authenticated tenant to inspect; may be {@code null}, in which case
	 *             the method returns {@code false} immediately.
	 * @return {@code true} if all integrity conditions are met; {@code false} otherwise.
	 */
	public boolean initialEmbeddedCheck(T user) {

		if(user == null) return false;

		final long userID = user.getId();
		final String uid = user.getUid();

		return !Pastebox.isEmptyString(uid) && NVerseValidatorUtilities.isGenericNumericIDValid(userID);
	}
}

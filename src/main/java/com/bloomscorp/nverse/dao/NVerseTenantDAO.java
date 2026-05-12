package com.bloomscorp.nverse.dao;

import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;

/**
 * Persistence adapter interface for tenant (user account) operations in the NVerse model.
 *
 * <p>Consuming applications implement this interface once, typically as a Spring
 * {@code @Repository} that delegates to a JPA repository or JDBC template. The
 * implementation is injected into {@link com.bloomscorp.nverse.NVerseUserDetailsService}
 * and used by the NVerse authentication flow to look up and create tenant accounts.</p>
 *
 * @param <T> the concrete tenant entity type, must implement {@link NVerseTenant}.
 * @param <E> the role enum type.
 * @param <R> the concrete role entity type, must implement {@link NVerseRole}.
 */
public interface NVerseTenantDAO<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	/**
	 * Checks whether an email address is unique (not already registered) in the database.
	 *
	 * @param email   the email address to check; may be in plaintext or AES-encrypted form
	 *                depending on the {@code encoded} flag.
	 * @param encoded {@code true} if {@code email} is already AES-encrypted and can be
	 *                compared directly against stored values; {@code false} if {@code email}
	 *                is plaintext and must be encrypted before comparison.
	 * @return {@code true} if no tenant record with the given email exists; {@code false} otherwise.
	 */
	boolean verifyUniqueEmail(String email, boolean encoded);

	/**
	 * Checks whether a contact number is unique (not already registered) in the database.
	 *
	 * @param contactNumber the phone number to check.
	 * @return {@code true} if no tenant record with the given contact number exists;
	 *         {@code false} otherwise.
	 */
	boolean verifyUniqueContactNumber(String contactNumber);

	/**
	 * Retrieves a tenant entity by its AES-encrypted email address.
	 *
	 * <p>This is the primary lookup method used by the authentication flow. The
	 * {@code username} parameter corresponds to the encrypted form of the user's
	 * email address, as stored in the database.</p>
	 *
	 * @param username the AES-encrypted email address to look up.
	 * @return the matching tenant entity, or {@code null} if no record is found.
	 */
	T retrieveUserByEmail(String username);

	/**
	 * Persists a new tenant along with an initial role assignment.
	 *
	 * <p>This overload applies default uniqueness checks — both email and contact
	 * number are verified for uniqueness before insertion.</p>
	 *
	 * @param user     the tenant entity to persist; must not be {@code null}.
	 * @param userRole the initial role entity to assign to the new tenant; must not be {@code null}.
	 * @return an {@link com.bloomscorp.hastar.code.ActionCode} integer indicating the outcome
	 *         ({@code INSERT_SUCCESS}, {@code NOT_UNIQUE}, {@code INSERT_FAILURE}, etc.).
	 */
	int addNewTenant(T user, R userRole);

	/**
	 * Persists a new tenant along with an initial role assignment, with explicit control
	 * over which uniqueness checks are performed.
	 *
	 * @param user                       the tenant entity to persist; must not be {@code null}.
	 * @param userRole                   the initial role entity to assign; must not be {@code null}.
	 * @param verifyUniqueEmail          {@code true} to check that the email address is not
	 *                                   already registered before inserting.
	 * @param verifyUniqueContactNumber  {@code true} to check that the contact number is not
	 *                                   already registered before inserting.
	 * @return an {@link com.bloomscorp.hastar.code.ActionCode} integer indicating the outcome.
	 */
	int addNewTenant(T user, R userRole, boolean verifyUniqueEmail, boolean verifyUniqueContactNumber);
}

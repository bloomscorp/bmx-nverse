package com.bloomscorp.nverse.pojo;

import java.util.List;

/**
 * Core contract for user/tenant entities in the NVerse security model.
 *
 * <p>Every application that integrates bmx-nverse must provide at least one JPA entity
 * that implements this interface. NVerse relies on these accessors to construct
 * {@link com.bloomscorp.nverse.NVerseUser} objects for Spring Security, perform
 * account-state checks in the filter chain, and drive role-based authorization in
 * {@link com.bloomscorp.nverse.NVerseGatekeeper}.</p>
 *
 * <p>The email field is expected to be stored in AES-encrypted form in the database;
 * the {@link com.bloomscorp.nverse.NVerseEmailEncoder} handles encryption and
 * decryption transparently during authentication.</p>
 *
 * @param <E> the application-specific role enum type.
 * @param <R> the concrete role entity type, must implement {@link NVerseRole}.
 */
public interface NVerseTenant<E extends Enum<E>, R extends NVerseRole<E>> {

	/**
	 * Returns the database primary key for this tenant.
	 *
	 * @return the unique numeric identifier; must be {@code > 0} for a valid tenant.
	 */
	Long getId();

	/**
	 * Returns the universally unique identifier (UID) for this tenant.
	 *
	 * @return the UID string; must not be {@code null} or blank for a valid tenant.
	 */
	String getUid();

	/**
	 * Sets the universally unique identifier for this tenant.
	 *
	 * @param uid the UID string to assign; must not be {@code null} or blank.
	 */
	void setUid(String uid);

	/**
	 * Returns the AES-encrypted email address stored for this tenant.
	 *
	 * @return the encrypted email string as persisted in the database.
	 */
	String getEmail();

	/**
	 * Sets the AES-encrypted email address for this tenant.
	 *
	 * @param email the encrypted email string to persist.
	 */
	void setEmail(String email);

	/**
	 * Returns the contact phone number for this tenant.
	 *
	 * @return the contact number string; may be {@code null} if not collected.
	 */
	String getContactNumber();

	/**
	 * Returns the BCrypt-encoded (peppered) password for this tenant.
	 *
	 * @return the encoded password string as persisted in the database.
	 */
	String getPassword();

	/**
	 * Sets the BCrypt-encoded (peppered) password for this tenant.
	 *
	 * @param password the encoded password string to persist.
	 */
	void setPassword(String password);

	/**
	 * Returns whether this tenant account is active and permitted to authenticate.
	 *
	 * @return {@code true} if the account is active; {@code false} if it has been
	 *         deactivated and should be treated as disabled by Spring Security.
	 */
	boolean isActive();

	/**
	 * Sets the active state of this tenant account.
	 *
	 * @param active {@code true} to enable the account, {@code false} to disable it.
	 */
	void setActive(boolean active);

	/**
	 * Returns whether this tenant account has been soft-deleted.
	 *
	 * <p>Deleted accounts are treated as having expired credentials by
	 * {@link com.bloomscorp.nverse.NVerseUser}, preventing authentication.</p>
	 *
	 * @return {@code true} if the account is marked as deleted.
	 */
	boolean isDeleted();

	/**
	 * Returns whether this tenant account is currently suspended.
	 *
	 * <p>Suspended accounts fail the credentials-non-expired check in
	 * {@link com.bloomscorp.nverse.NVerseUser}.</p>
	 *
	 * @return {@code true} if the account is suspended.
	 */
	boolean isSuspended();

	/**
	 * Returns the authentication provider that was used to create this tenant account.
	 *
	 * @return the {@link NVERSE_AUTH_PROVIDER} constant; must not be {@code null}.
	 */
	NVERSE_AUTH_PROVIDER getProvider();

	/**
	 * Returns the list of roles assigned to this tenant.
	 *
	 * <p>These are mapped to Spring Security {@code GrantedAuthority} objects when
	 * constructing the {@link com.bloomscorp.nverse.NVerseUser} wrapper.</p>
	 *
	 * @return the list of role entities; must not be {@code null}, may be empty.
	 */
	List<R> getRoles();

	/**
	 * Sets the Unix-epoch creation timestamp for this tenant.
	 *
	 * @param creationTime the account creation time in milliseconds since epoch.
	 */
	void setCreationTime(long creationTime);

	/**
	 * Sets the URL of the tenant's profile image.
	 *
	 * @param profileImageUrl the absolute URL of the profile image, or {@code null}
	 *                        if no image has been set.
	 */
	void setProfileImageUrl(String profileImageUrl);
}

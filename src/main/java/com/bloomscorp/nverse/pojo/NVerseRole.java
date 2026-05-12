package com.bloomscorp.nverse.pojo;

/**
 * Marker interface for role entities in the NVerse security model.
 *
 * <p>Every concrete role JPA entity in a consuming application must implement this
 * interface. NVerse uses the generic type parameter {@code E} to bind the role to
 * a specific application-defined enum, ensuring type-safe role checks throughout
 * the security and gatekeeper layers.</p>
 *
 * @param <E> the application-specific enum type whose constants represent the
 *            individual role values (e.g., {@code ROLE_ADMIN}, {@code ROLE_USER}).
 *            Must extend {@link Enum}.
 */
public interface NVerseRole<
	E extends Enum<E>
> {

	/**
	 * Returns the database primary key of this role entity.
	 *
	 * @return the unique identifier; must not be {@code null} or {@code <= 0}.
	 */
	Long getId();

	/**
	 * Returns the enum constant that represents the actual role value.
	 *
	 * @return the role enum constant; must not be {@code null}.
	 */
	E getRole();
}

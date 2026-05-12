package com.bloomscorp.nverse.dao;

import com.bloomscorp.nverse.pojo.NVerseRole;

/**
 * Persistence adapter interface for user role operations in the NVerse model.
 *
 * <p>Consuming applications implement this interface to provide role persistence
 * independently of tenant persistence. This separation allows roles to be managed
 * (added, updated, audited) without going through the tenant DAO, supporting use cases
 * such as role elevation, role history tracking, or multi-role assignments.</p>
 *
 * @param <E> the role enum type.
 * @param <R> the concrete role entity type, must implement {@link NVerseRole}.
 */
public interface NVerseUserRoleDAO<
    E extends Enum<E>,
    R extends NVerseRole<E>
> {

	/**
	 * Persists a new role entity for a user.
	 *
	 * @param role the role entity to insert; must not be {@code null} and must have its
	 *             associated tenant reference populated before calling this method.
	 * @return an {@link com.bloomscorp.hastar.code.ActionCode} integer indicating the
	 *         outcome ({@code INSERT_SUCCESS} or {@code INSERT_FAILURE}).
	 */
    int addNewRole(R role);
}

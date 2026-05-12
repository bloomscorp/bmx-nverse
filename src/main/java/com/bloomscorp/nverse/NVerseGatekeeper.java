package com.bloomscorp.nverse;

import com.bloomscorp.hastar.code.ActionCode;
import com.bloomscorp.hastar.code.ErrorCode;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract authorization guard that provides role-based access control and embedded
 * integrity checks for protected operations.
 *
 * <p>{@code NVerseGatekeeper} is the primary mechanism for enforcing <em>what</em> an
 * authenticated user is allowed to do within the application. Consuming applications
 * subclass it and implement {@link #userHasAppropriateAuthority} to define the custom
 * permission logic for their own operation codes.</p>
 *
 * <p>Two built-in role hierarchies are recognized by their enum constant name:</p>
 * <ul>
 *   <li><strong>{@code ROLE_GOD_MODE}</strong> — the highest privilege. A user with this
 *       role passes all role checks.</li>
 *   <li><strong>{@code ROLE_SUPER_USER}</strong> — elevated privilege. Passes the
 *       {@link #roleSU} check, which also accepts {@code ROLE_GOD_MODE} users.</li>
 * </ul>
 *
 * <p>The primary entry point for operation-level checks is {@link #runSurveillance},
 * which first verifies that the user is not a forged request (via
 * {@link NVerseSecureLayer#initialEmbeddedCheck}), then delegates to
 * {@link #userHasAppropriateAuthority} for the application-specific permission check.
 * The result is returned as an {@link NVerseSurveillanceReport}.</p>
 *
 * @param <T> the concrete tenant type, must implement {@link NVerseTenant}.
 * @param <E> the role enum type whose constants include {@code ROLE_GOD_MODE} and
 *            {@code ROLE_SUPER_USER} at minimum.
 * @param <R> the concrete role entity type, must implement {@link NVerseRole}.
 *
 * @see NVerseSecureLayer
 * @see NVerseSurveillanceReport
 */
@AllArgsConstructor
public abstract class NVerseGatekeeper<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	private static final String ROLE_GOD_MODE = "ROLE_GOD_MODE";
	private static final String ROLE_SUPER_USER = "ROLE_SUPER_USER";

	private final NVerseSecureLayer<T, E, R> secureLayer;

	/**
	 * Returns {@code true} if the given user has the {@code ROLE_GOD_MODE} role, granting
	 * unrestricted access to all operations.
	 *
	 * @param user the tenant to check; must not be {@code null}.
	 * @return {@code true} if any of the user's roles has the name {@code "ROLE_GOD_MODE"}.
	 */
	public boolean roleGOD(@NotNull T user) {
		return user
			.getRoles()
			.stream()
			.parallel()
			.map(NVerseRole::getRole)
			.anyMatch(e -> e.name().equals(ROLE_GOD_MODE));
	}

	/**
	 * Returns {@code true} if the given user has either {@code ROLE_GOD_MODE} or
	 * {@code ROLE_SUPER_USER}, granting elevated access.
	 *
	 * <p>{@code ROLE_GOD_MODE} implicitly satisfies this check — a GOD user is always
	 * also a super user.</p>
	 *
	 * @param user the tenant to check.
	 * @return {@code true} if the user has {@code ROLE_GOD_MODE} or {@code ROLE_SUPER_USER}.
	 */
	public boolean roleSU(T user) {
		return this.roleGOD(user) ||
			user.getRoles()
				.stream()
				.parallel()
				.map(NVerseRole::getRole)
				.anyMatch(e -> e.name().equals(ROLE_SUPER_USER));
	}

	/**
	 * Runs the full NVerse surveillance pipeline for the given user and operation code.
	 *
	 * <p>The pipeline consists of two sequential checks:</p>
	 * <ol>
	 *   <li><strong>Embedded integrity check</strong> — {@link NVerseSecureLayer#initialEmbeddedCheck}
	 *       verifies that the user object is not {@code null}, has a positive {@code id},
	 *       and has a non-blank {@code uid}. Failure returns a report with
	 *       {@link ErrorCode#FORGED_REQUEST}.</li>
	 *   <li><strong>Authority check</strong> — {@link #userHasAppropriateAuthority} is called
	 *       with the user and operation code. Failure returns a report with
	 *       {@link ErrorCode#AUTHORIZATION_DENIED}.</li>
	 * </ol>
	 *
	 * @param user the authenticated tenant performing the operation.
	 * @param code the application-defined integer operation code to authorize.
	 * @return an {@link NVerseSurveillanceReport} with {@code failed = false} and
	 *         {@code errorCode = }{@link ActionCode#NO_ACTION} if all checks pass;
	 *         otherwise a report with {@code failed = true} and the appropriate error code.
	 */
	public NVerseSurveillanceReport runSurveillance(T user, int code) {
		if (!this.secureLayer.initialEmbeddedCheck(user))
			return new NVerseSurveillanceReport(true, ErrorCode.FORGED_REQUEST);
		if (!this.userHasAppropriateAuthority(user, code))
			return new NVerseSurveillanceReport(true, ErrorCode.AUTHORIZATION_DENIED);
		return new NVerseSurveillanceReport(false, ActionCode.NO_ACTION);
	}

	/**
	 * Application-defined permission check for the given operation code.
	 *
	 * <p>Implementing classes should return {@code true} if {@code user} is permitted to
	 * perform the operation identified by {@code code}, and {@code false} if access must be
	 * denied. The implementation may call {@link #roleGOD} or {@link #roleSU} as part of its
	 * logic, but is not required to.</p>
	 *
	 * @param user the authenticated tenant requesting the operation.
	 * @param code the application-defined integer operation code being checked.
	 * @return {@code true} if the user is authorized; {@code false} to deny access.
	 */
	public abstract boolean userHasAppropriateAuthority(T user, int code);
}

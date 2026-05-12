package com.bloomscorp.nverse;

import com.bloomscorp.hastar.AuthorizationException;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.AllArgsConstructor;

/**
 * Resolves a fully loaded tenant entity from a raw Authorization header value.
 *
 * <p>{@code NVerseAuthorityResolver} is a convenience component for controllers or
 * services that need to obtain the authenticated tenant object from the request context
 * without going through Spring Security's {@link org.springframework.security.core.context.SecurityContextHolder}.
 * It extracts the username (encrypted email) embedded in the JWT, then delegates to
 * {@link NVerseUserDetailsService} to load and return the full tenant entity.</p>
 *
 * @param <T> the concrete tenant type, must implement {@link NVerseTenant}.
 * @param <E> the role enum type.
 * @param <R> the concrete role entity type, must implement {@link NVerseRole}.
 */
@AllArgsConstructor
public class NVerseAuthorityResolver<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	private final NVerseJWTService<T, E, R> jwtService;
	private final NVerseUserDetailsService<T, E, R> userDetailsService;

	/**
	 * Resolves the tenant entity associated with the given raw Authorization header value.
	 *
	 * <p>The method strips the {@code "Bearer "} prefix (characters 0–6) from the header,
	 * extracts the username claim from the resulting JWT, then loads and returns the
	 * corresponding tenant via {@link NVerseUserDetailsService#loadNVerseUserByEncryptedUsername}.</p>
	 *
	 * @param authorizationToken the full value of the HTTP {@code Authorization} header,
	 *                           expected to start with {@code "Bearer "}. If {@code null},
	 *                           an {@link AuthorizationException} with code
	 *                           {@link AuthorizationException#AECx02} is thrown immediately.
	 * @return the fully loaded {@link NVerseTenant} entity associated with the JWT subject.
	 * @throws AuthorizationException if the {@code authorizationToken} is {@code null}
	 *                                (code {@link AuthorizationException#AECx02}), or if
	 *                                subsequent token parsing or user lookup fails.
	 */
	public T resolveUserInformationFromAuthorizationToken(String authorizationToken) {

		if (authorizationToken == null)
			throw new AuthorizationException(AuthorizationException.AECx02);

		return this.userDetailsService.loadNVerseUserByEncryptedUsername(
			this.jwtService.getUsernameFromToken(
				authorizationToken.substring(7)
			)
		).getTenant();
	}
}

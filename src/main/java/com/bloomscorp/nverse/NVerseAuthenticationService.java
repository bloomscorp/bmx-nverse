package com.bloomscorp.nverse;

import com.bloomscorp.nverse.pojo.NVERSE_AUTH_PROVIDER;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import com.bloomscorp.pastebox.Pastebox;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Utility service providing authentication and auth-provider validation operations
 * used at login endpoints.
 *
 * <p>This class is intentionally not annotated as a Spring bean — consuming applications
 * instantiate it directly or register it themselves. It wraps two discrete operations:
 * delegating username/password authentication to Spring Security's
 * {@link AuthenticationManager}, and checking that a tenant's stored provider matches
 * the provider declared in an incoming request.</p>
 */
public class NVerseAuthenticationService {

	/**
	 * Authenticates the given username and password by delegating to the supplied
	 * {@link AuthenticationManager}.
	 *
	 * <p>Spring Security will invoke the configured {@link NVerseUserDetailsService}
	 * and {@link NVersePasswordEncoder} internally during this call. If authentication
	 * fails, one of the declared exceptions is thrown and propagated to the caller
	 * (typically a login controller method).</p>
	 *
	 * @param username              the plaintext email address supplied by the user.
	 * @param password              the plaintext password supplied by the user.
	 * @param authenticationManager the {@link AuthenticationManager} to delegate to;
	 *                              must not be {@code null}.
	 * @throws DisabledException      if the account matching {@code username} is disabled.
	 * @throws BadCredentialsException if the password does not match the stored hash.
	 */
	public void authenticate(
		String username,
		String password,
		@NotNull AuthenticationManager authenticationManager
	) throws DisabledException, BadCredentialsException {
		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(
				username,
				password
			)
		);
	}

	/**
	 * Verifies that the given tenant's stored authentication provider matches the
	 * expected {@code provider} value.
	 *
	 * <p>When {@code allowUnknown} is {@code false} (the typical production case), the
	 * match is only successful if the tenant's provider equals {@code provider} AND is
	 * not {@link NVERSE_AUTH_PROVIDER#UNKNOWN}. Passing {@code true} for
	 * {@code allowUnknown} relaxes the check to a simple equality test, which can be
	 * useful during migration or testing scenarios.</p>
	 *
	 * @param <T>          the concrete tenant type.
	 * @param <E>          the role enum type.
	 * @param <R>          the concrete role entity type.
	 * @param tenant       the tenant whose provider is being validated; must not be {@code null}.
	 * @param provider     the expected {@link NVERSE_AUTH_PROVIDER} value from the request.
	 *                     Returns {@code false} if {@code null} or not a valid enum constant.
	 * @param allowUnknown {@code true} to allow a match even when the provider is
	 *                     {@link NVERSE_AUTH_PROVIDER#UNKNOWN}; {@code false} to reject it.
	 * @return {@code true} if the tenant's provider matches {@code provider} under the
	 *         specified leniency rule; {@code false} otherwise.
	 */
	public <T extends NVerseTenant<E, R>,
		E extends Enum<E>,
		R extends NVerseRole<E>>
	boolean validateAuthProvider(
		@NotNull T tenant,
		NVERSE_AUTH_PROVIDER provider,
		Boolean allowUnknown
	) {
		if (provider == null || !Pastebox.isInEnum(provider, NVERSE_AUTH_PROVIDER.class)) return false;

		if (allowUnknown) {
			return tenant.getProvider() == provider;
		} else {
			return tenant.getProvider() == provider && tenant.getProvider() != NVERSE_AUTH_PROVIDER.UNKNOWN;
		}
	}
}

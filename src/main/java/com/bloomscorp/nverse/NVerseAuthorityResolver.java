package com.bloomscorp.nverse;

import com.bloomscorp.hastar.AuthorizationException;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NVerseAuthorityResolver<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	private final NVerseJWTService<T, E, R> jwtService;
	private final NVerseUserDetailsService<T, E, R> userDetailsService;

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

package com.bloomscorp.nverse;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class NVerseAuthenticationService {

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
}

package com.bloomscorp.nverse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Custom {@link AuthenticationEntryPoint} that re-throws the received
 * {@link AuthenticationException} instead of writing an error response directly.
 *
 * <p>Spring Security invokes this entry point when an unauthenticated request reaches
 * a secured endpoint. By rethrowing the exception, NVerse delegates all response
 * writing to {@link NVerseExceptionHandlerFilter}, which is positioned earlier in the
 * filter chain and handles exception-to-response mapping in one centralized place.</p>
 *
 * @see NVerseExceptionHandlerFilter
 * @see NVerseSecurityFilterChain
 */
public class NVerseAuthenticationEntryPoint implements AuthenticationEntryPoint {

	/**
	 * Re-throws the supplied {@link AuthenticationException} so that
	 * {@link NVerseExceptionHandlerFilter} can catch and convert it to a structured
	 * JSON failure response.
	 *
	 * @param request   the incoming HTTP request that triggered the authentication failure.
	 * @param response  the HTTP response (not written to by this implementation).
	 * @param e         the authentication exception that caused the entry point to be invoked;
	 *                  must not be {@code null}.
	 * @throws AuthenticationException always — re-throws {@code e} unconditionally.
	 */
	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		@NotNull AuthenticationException e
	) {
		throw e;
	}
}

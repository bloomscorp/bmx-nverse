package com.bloomscorp.nverse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * A {@link OncePerRequestFilter} that wraps the incoming {@link HttpServletRequest}
 * in an {@link NVerseHttpRequestWrapper} before passing it down the filter chain.
 *
 * <p>This filter is registered after the Spring Security authorization filter in
 * {@link NVerseSecurityFilterChain}, ensuring that every request reaching the
 * application layer carries a replayable body stream. Downstream components —
 * particularly the sanitizer layer and Spring MVC argument resolvers — can safely
 * read the request body multiple times without exhausting the original servlet stream.</p>
 *
 * @see NVerseHttpRequestWrapper
 * @see NVerseSecurityFilterChain
 */
public class NVerseHttpRequestFilter extends OncePerRequestFilter {

	/**
	 * Replaces the original {@link HttpServletRequest} with an {@link NVerseHttpRequestWrapper}
	 * and continues the filter chain.
	 *
	 * @param httpServletRequest  the original incoming request.
	 * @param httpServletResponse the response object, passed through unchanged.
	 * @param filterChain         the remainder of the filter chain to invoke.
	 * @throws ServletException if a downstream filter or servlet throws a servlet error.
	 * @throws IOException      if an I/O error occurs while constructing the wrapper or
	 *                          processing the chain.
	 */
	@Override
	protected void doFilterInternal(
		@NotNull HttpServletRequest httpServletRequest,
		@NotNull HttpServletResponse httpServletResponse,
		@NotNull FilterChain filterChain
	) throws ServletException, IOException {
		filterChain.doFilter(
			new NVerseHttpRequestWrapper(httpServletRequest),
			httpServletResponse
		);
	}
}

package com.bloomscorp.nverse;

import com.bloomscorp.hastar.AuthorizationException;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import com.bloomscorp.nverse.support.Constant;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * A {@link OncePerRequestFilter} that performs JWT-based stateless authentication on
 * every incoming HTTP request that carries an {@code Authorization: Bearer} header.
 *
 * <p>For each request this filter:</p>
 * <ol>
 *   <li>Rejects the request immediately with an {@link AuthorizationException} if
 *       the Spring Security context already holds a non-null authentication object
 *       (code {@link AuthorizationException#AECx04}), preventing double-authentication.</li>
 *   <li>Inspects the {@code Authorization} header. If present, delegates to
 *       {@link #startAuthentication} to validate the token and populate the security
 *       context.</li>
 *   <li>If no {@code Authorization} header is present the request is forwarded
 *       unauthenticated — secured endpoints will subsequently be rejected by Spring
 *       Security's authorization layer, not by this filter.</li>
 * </ol>
 *
 * <p>This filter must be registered <em>before</em>
 * {@link org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter}
 * and <em>after</em> {@link NVerseExceptionHandlerFilter} in the security filter chain,
 * so that any exceptions it throws are caught and converted to JSON responses by the
 * exception handler filter.</p>
 *
 * @param <T> the concrete tenant type, must implement {@link NVerseTenant}.
 * @param <E> the role enum type.
 * @param <R> the concrete role entity type, must implement {@link NVerseRole}.
 *
 * @see NVerseJWTService
 * @see NVerseUserDetailsService
 * @see NVerseExceptionHandlerFilter
 */
@AllArgsConstructor
public class NVerseRequestFilter<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> extends OncePerRequestFilter {

	private final NVerseJWTService<T, E, R> jwtService;
	private final NVerseUserDetailsService<T, E, R> userDetailsService;

	/**
	 * Validates the JWT token from the {@code Authorization} header (if present) and
	 * populates the Spring Security context before continuing the filter chain.
	 *
	 * @param httpServletRequest  the incoming HTTP request.
	 * @param httpServletResponse the HTTP response, passed through to the chain.
	 * @param filterChain         the remainder of the filter chain to invoke.
	 * @throws ServletException            if a downstream filter or servlet throws a servlet error.
	 * @throws IOException                 if an I/O error occurs during chain processing.
	 * @throws IllegalArgumentException    if the JWT token contains illegal characters.
	 * @throws ExpiredJwtException         if the JWT token has expired.
	 * @throws AuthorizationException      if the security context is already authenticated
	 *                                     (code {@link AuthorizationException#AECx04}), the
	 *                                     Authorization header is missing the Bearer prefix
	 *                                     ({@link AuthorizationException#AECx02}), the username
	 *                                     claim is absent ({@link AuthorizationException#AECx03}),
	 *                                     or the token fails validation
	 *                                     ({@link AuthorizationException#AECx05}).
	 */
	@Override
	protected void doFilterInternal(
		@NotNull HttpServletRequest httpServletRequest,
		@NotNull HttpServletResponse httpServletResponse,
		@NotNull FilterChain filterChain
	) throws ServletException,
		IOException,
		IllegalArgumentException,
		ExpiredJwtException,
		AuthorizationException {

		if(SecurityContextHolder.getContext().getAuthentication() != null)
			throw new AuthorizationException(AuthorizationException.AECx04);

		String authorizationRequestHeader = httpServletRequest.getHeader(Constant.REQUEST_HEADER_AUTHORIZATION);

		if(authorizationRequestHeader != null)
			this.startAuthentication(authorizationRequestHeader, httpServletRequest);

		try {
			filterChain.doFilter(httpServletRequest, httpServletResponse);
		} catch (InternalAuthenticationServiceException e) {
			// TODO: check what exception to throw and handle for username not found during authorization
//			if(e.getCause() instanceof EmptyDatasetException)
//				throw new UsernameNotFoundException(Constant.MSG_USERNAME_NOT_FOUND);
			throw e;
		}
	}

	/**
	 * Extracts, validates, and authenticates the JWT token from the given Authorization
	 * header value, then sets the resulting {@link UsernamePasswordAuthenticationToken}
	 * in the Spring Security context.
	 *
	 * @param authorizationRequestHeader the full value of the {@code Authorization} header;
	 *                                   must start with {@code "Bearer "}.
	 * @param httpServletRequest         the current HTTP request, used to build authentication
	 *                                   details via {@link WebAuthenticationDetailsSource}.
	 * @throws AuthorizationException if the header does not start with the Bearer prefix
	 *                                ({@link AuthorizationException#AECx02}), the username
	 *                                claim extracted from the token is {@code null}
	 *                                ({@link AuthorizationException#AECx03}), or the token
	 *                                fails signature/expiry validation
	 *                                ({@link AuthorizationException#AECx05}).
	 */
	private void startAuthentication(@NotNull String authorizationRequestHeader, HttpServletRequest httpServletRequest) {

		if(!authorizationRequestHeader.startsWith(Constant.REQUEST_HEADER_VALUE_BEARER))
			throw new AuthorizationException(AuthorizationException.AECx02);

		String jwtToken = authorizationRequestHeader.substring(7);
		String username = this.jwtService.getUsernameFromToken(jwtToken);

		if(username == null)
			throw new AuthorizationException(AuthorizationException.AECx03);

		UserDetails userDetails = this.userDetailsService.loadUserByEncryptedUsername(username);

		if(!jwtService.validateToken(jwtToken, userDetails))
			throw new AuthorizationException(AuthorizationException.AECx05);

		UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
			userDetails,
			null,
			userDetails.getAuthorities()
		);

		usernamePasswordAuthenticationToken.setDetails(
			new WebAuthenticationDetailsSource().buildDetails(httpServletRequest)
		);

		SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
	}
}

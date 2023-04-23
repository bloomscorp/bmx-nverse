package com.bloomscorp.nverse;

import com.bloomscorp.hastar.AuthorizationException;
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

@AllArgsConstructor
public class NVerseRequestFilter<
	T extends NVerseTenant<E>,
	E extends Enum<E>
> extends OncePerRequestFilter {

	private final NVerseJWTService<T, E> jwtService;
	private final NVerseUserDetailsService<T, E> userDetailsService;

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

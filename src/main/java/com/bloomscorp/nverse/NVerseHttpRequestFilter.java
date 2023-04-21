package com.bloomscorp.nverse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class NVerseHttpRequestFilter extends OncePerRequestFilter {

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

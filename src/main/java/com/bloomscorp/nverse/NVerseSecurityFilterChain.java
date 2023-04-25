package com.bloomscorp.nverse;

import com.bloomscorp.alfred.LogBook;
import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.Log;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class NVerseSecurityFilterChain {}
//<
//	B extends LogBook<L, A, T, E>,
//	L extends Log,
//	A extends AuthenticationLog,
//	T extends NVerseTenant<E, R>,
//	E extends Enum<E>,
//	R extends NVerseRole<E>
//> {
//
//	public SecurityFilterChain filterChain(
//		@NotNull HttpSecurity httpSecurity,
//		String[] nonAuthenticatedURLs,
//		String postAntMatcher,
//		NVerseAuthenticationEntryPoint authenticationEntryPoint,
//		NVerseRequestFilter<T, E, R> requestFilter,
//		NVerseExceptionHandlerFilter<B, L, A, T, E, R> exceptionHandlerFilter,
//		NVerseHttpRequestFilter httpRequestFilter
//	) throws Exception {
//
//		httpSecurity
//			.requiresChannel()
//			.anyRequest()
//			.requiresSecure()
//			.and()
//			.csrf()
//			.disable()
//			.cors()
//			.and()
//			.authorizeHttpRequests()
//			.requestMatchers(nonAuthenticatedURLs)
//			.permitAll()
//			.anyRequest()
//			.authenticated()
//			.and()
//			.exceptionHandling()
//			.authenticationEntryPoint(authenticationEntryPoint)
//			.and()
//			.sessionManagement()
//			.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//			.and()
//			.addFilterBefore(requestFilter, UsernamePasswordAuthenticationFilter.class)
//			.addFilterBefore(exceptionHandlerFilter, NVerseRequestFilter.class)
//			.securityMatcher(postAntMatcher)
//			.addFilterAfter(httpRequestFilter, AuthorizationFilter.class)
//			.headers()
//			.frameOptions()
//			.disable();
//
//		return httpSecurity.build();
//	}
//}

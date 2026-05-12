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

/**
 * Factory class that assembles the NVerse opinionated Spring Security filter chain.
 *
 * <p>Consuming applications call {@link #filterChain} from within a
 * {@code @Bean}-annotated method inside a {@code @Configuration} class to produce the
 * {@link SecurityFilterChain} that enforces all NVerse security policies. The assembled
 * chain applies the following rules:</p>
 *
 * <ul>
 *   <li><strong>HTTPS enforcement</strong> — all requests are required to use a secure
 *       channel ({@code requiresSecure()}).</li>
 *   <li><strong>CSRF disabled</strong> — stateless JWT authentication makes CSRF tokens
 *       unnecessary.</li>
 *   <li><strong>CORS enabled</strong> — uses the {@link NVerseCORSConfigurationSource}
 *       configured by the application.</li>
 *   <li><strong>URL authorization</strong> — the supplied {@code nonAuthenticatedURLs}
 *       are permitted without a token; all other URLs require authentication.</li>
 *   <li><strong>Custom entry point</strong> — {@link NVerseAuthenticationEntryPoint}
 *       re-throws authentication exceptions for centralized handling.</li>
 *   <li><strong>Stateless session management</strong> — Spring Security never creates
 *       an {@code HttpSession}.</li>
 *   <li><strong>Filter ordering</strong>:
 *     <ol>
 *       <li>{@link NVerseExceptionHandlerFilter} — before {@link NVerseRequestFilter},
 *           catches all security exceptions and converts them to JSON responses.</li>
 *       <li>{@link NVerseRequestFilter} — before
 *           {@link UsernamePasswordAuthenticationFilter}, performs JWT validation.</li>
 *       <li>{@link NVerseHttpRequestFilter} — after {@link AuthorizationFilter},
 *           wraps the request for repeatable body reads.</li>
 *     </ol>
 *   </li>
 *   <li><strong>Frame options disabled</strong> — allows iframing where needed (can be
 *       re-enabled at the application level if required).</li>
 * </ul>
 *
 * @param <B> the concrete {@link LogBook} subclass used for logging.
 * @param <L> the concrete log JPA entity type.
 * @param <A> the concrete authentication log JPA entity type.
 * @param <T> the concrete tenant type, must implement {@link NVerseTenant}.
 * @param <E> the role enum type.
 * @param <R> the concrete role entity type, must implement {@link NVerseRole}.
 */
public class NVerseSecurityFilterChain<
	B extends LogBook<L, A, T, E, R>,
	L extends Log,
	A extends AuthenticationLog,
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	/**
	 * Configures and builds the {@link SecurityFilterChain} with all NVerse policies applied.
	 *
	 * @param httpSecurity              the Spring Security {@link HttpSecurity} builder to configure;
	 *                                  must not be {@code null}.
	 * @param nonAuthenticatedURLs      an array of URL patterns that are publicly accessible
	 *                                  without a JWT token (e.g., {@code "/api/auth/**"}).
	 * @param postAntMatcher            the Ant-style URL pattern passed to
	 *                                  {@link HttpSecurity#securityMatcher} to scope this
	 *                                  filter chain.
	 * @param authenticationEntryPoint  the {@link NVerseAuthenticationEntryPoint} that handles
	 *                                  unauthenticated-access failures.
	 * @param requestFilter             the {@link NVerseRequestFilter} for JWT validation.
	 * @param exceptionHandlerFilter    the {@link NVerseExceptionHandlerFilter} for centralized
	 *                                  exception-to-JSON response conversion.
	 * @param httpRequestFilter         the {@link NVerseHttpRequestFilter} for repeatable request
	 *                                  body reading.
	 * @return the fully configured and built {@link SecurityFilterChain}.
	 * @throws Exception if Spring Security configuration fails.
	 */
	public SecurityFilterChain filterChain(
		@NotNull HttpSecurity httpSecurity,
		String[] nonAuthenticatedURLs,
		String postAntMatcher,
		NVerseAuthenticationEntryPoint authenticationEntryPoint,
		NVerseRequestFilter<T, E, R> requestFilter,
		NVerseExceptionHandlerFilter<B, L, A, T, E, R> exceptionHandlerFilter,
		NVerseHttpRequestFilter httpRequestFilter
	) throws Exception {

		httpSecurity
			.requiresChannel()
			.anyRequest()
			.requiresSecure()
			.and()
			.csrf()
			.disable()
			.cors()
			.and()
			.authorizeHttpRequests()
			.requestMatchers(nonAuthenticatedURLs)
			.permitAll()
			.anyRequest()
			.authenticated()
			.and()
			.exceptionHandling()
			.authenticationEntryPoint(authenticationEntryPoint)
			.and()
			.sessionManagement()
			.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			.and()
			.addFilterBefore(requestFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(exceptionHandlerFilter, NVerseRequestFilter.class)
			.securityMatcher(postAntMatcher)
			.addFilterAfter(httpRequestFilter, AuthorizationFilter.class)
			.headers()
			.frameOptions()
			.disable();

		return httpSecurity.build();
	}
}

package com.bloomscorp.nverse;

import com.bloomscorp.alfred.LogBook;
import com.bloomscorp.alfred.cron.CronManager;
import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.Log;
import com.bloomscorp.alfred.support.ReporterID;
import com.bloomscorp.hastar.AuthorizationException;
import com.bloomscorp.hastar.CarrierException;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import com.bloomscorp.nverse.support.Constant;
import com.bloomscorp.nverse.support.Message;
import com.bloomscorp.raintree.RainTree;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * A {@link OncePerRequestFilter} positioned at the front of the security filter chain
 * that catches all security- and authentication-related exceptions thrown by downstream
 * filters and converts them into structured JSON failure responses.
 *
 * <p>This filter acts as a unified error boundary for the entire NVerse filter stack.
 * It wraps the downstream {@code filterChain.doFilter} call in a {@code try/catch}
 * block covering the full set of exception types that the NVerse filters and Spring
 * Security may throw. Each exception type maps to a specific human-readable message
 * constant from {@link Message} or directly from the exception, written as a JSON
 * response via {@link RainTree}.</p>
 *
 * <p>All caught exceptions are also asynchronously logged through the {@link CronManager}
 * using {@link com.bloomscorp.alfred.cron.CronManager#scheduleExceptionLogTask}. In
 * non-production mode ({@code isProduction = false}) the exception stack trace is also
 * printed to standard error for local debugging.</p>
 *
 * <p>This filter must be registered <em>before</em> {@link NVerseRequestFilter} in the
 * security filter chain — see {@link NVerseSecurityFilterChain#filterChain}.</p>
 *
 * @param <B> the concrete {@link LogBook} subclass used by the application.
 * @param <L> the concrete log JPA entity type.
 * @param <A> the concrete authentication log JPA entity type.
 * @param <T> the concrete tenant type, must implement {@link NVerseTenant}.
 * @param <E> the role enum type.
 * @param <R> the concrete role entity type, must implement {@link NVerseRole}.
 *
 * @see NVerseRequestFilter
 * @see NVerseSecurityFilterChain
 * @see Message
 */
@AllArgsConstructor
public class NVerseExceptionHandlerFilter<
	B extends LogBook<L, A, T, E, R>,
	L extends Log,
	A extends AuthenticationLog,
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> extends OncePerRequestFilter {

	private final RainTree rainTree;
	private final CronManager<B, L, A, T, E, R> cron;
	private final boolean isProduction;

	/**
	 * Executes the filter chain and intercepts any authentication or authorization
	 * exception that propagates, converting it to a {@code 401 Unauthorized} JSON response.
	 *
	 * <p>The exception-to-message mapping is as follows:</p>
	 * <ul>
	 *   <li>{@link AuthorizationException} → {@code ex.getMessage()} (resolved from the error code).</li>
	 *   <li>{@link CarrierException} → {@code ex.getMessage()} (resolved from the wrapped error code).</li>
	 *   <li>{@link IllegalArgumentException} → {@link Message#ILLEGAL_ARGUMENTS}.</li>
	 *   <li>{@link ExpiredJwtException} → {@link Message#EXPIRED_CREDENTIALS}.</li>
	 *   <li>{@link ServletException} → {@link Message#SERVLET_FAILURE}.</li>
	 *   <li>{@link IOException} → {@link Message#IO_FAILURE}.</li>
	 *   <li>{@link BadCredentialsException} → {@link Message#BAD_CREDENTIALS}.</li>
	 *   <li>{@link DisabledException} → {@link Message#ACCOUNT_DISABLED}.</li>
	 *   <li>{@link InternalAuthenticationServiceException} → {@link Message#INTERNAL_AUTHENTICATION_SERVICE_FAILURE}.</li>
	 *   <li>{@link MalformedJwtException} → {@link Message#MALFORMED_CREDENTIALS}.</li>
	 *   <li>{@link LockedException} → {@link Message#USER_ACCOUNT_LOCKED}.</li>
	 *   <li>{@link UsernameNotFoundException} → {@link Message#USERNAME_NOT_FOUND}.</li>
	 *   <li>Any other {@link Exception} → {@link RainTree#failureResponse()} generic message.</li>
	 * </ul>
	 *
	 * @param httpServletRequest  the incoming HTTP request.
	 * @param httpServletResponse the HTTP response to write the error JSON body to.
	 * @param filterChain         the remainder of the filter chain.
	 * @throws IOException if writing the error response body fails.
	 */
	@Override
	protected void doFilterInternal(
		@NotNull HttpServletRequest httpServletRequest,
		@NotNull HttpServletResponse httpServletResponse,
		@NotNull FilterChain filterChain
	) throws IOException {

		boolean exceptionThrown = false;
		Exception exception = null;
		String methodName = "doFilterInternal";

		try {
			filterChain.doFilter(httpServletRequest, httpServletResponse);
		} catch(
			AuthorizationException 					|
			CarrierException 						|
			IllegalArgumentException 				|
			ExpiredJwtException 					|
			ServletException 						|
			IOException 							|
			BadCredentialsException 				|
			DisabledException 						|
			InternalAuthenticationServiceException 	|
			MalformedJwtException 					|
			LockedException 						|
			UsernameNotFoundException e
		) {

			exceptionThrown = true;
			exception = e;

			if(!this.isProduction)
				e.printStackTrace();

			if(e instanceof AuthorizationException)
				this.prepareResponseForException(
					httpServletResponse,
					rainTree.failureResponse(e.getMessage())
				);
			else if(e instanceof CarrierException)
				this.prepareResponseForException(
					httpServletResponse,
					rainTree.failureResponse(e.getMessage())
				);
			else if(e instanceof IllegalArgumentException)
				this.prepareResponseForException(
					httpServletResponse,
					rainTree.failureResponse(Message.ILLEGAL_ARGUMENTS)
				);
			else if(e instanceof ExpiredJwtException)
				this.prepareResponseForException(
					httpServletResponse,
					rainTree.failureResponse(Message.EXPIRED_CREDENTIALS)
				);
			else if(e instanceof ServletException)
				// TODO: check for the cause here, if cause exists, check for message
				this.prepareResponseForException(
					httpServletResponse,
					rainTree.failureResponse(Message.SERVLET_FAILURE)
				);
			else if(e instanceof IOException)
				this.prepareResponseForException(
					httpServletResponse,
					rainTree.failureResponse(Message.IO_FAILURE)
				);
			else if(e instanceof BadCredentialsException)
				this.prepareResponseForException(
					httpServletResponse,
					rainTree.failureResponse(Message.BAD_CREDENTIALS)
				);
			else if(e instanceof DisabledException)
				this.prepareResponseForException(
					httpServletResponse,
					rainTree.failureResponse(Message.ACCOUNT_DISABLED)
				);
				// TODO: check where to put this to use
//			else if(e instanceof EmptyDatasetException)
//				this.prepareResponseForException(httpServletResponse, rainTree.failureResponse(Message.AUTH_INFO_UNAVAILABLE));
			else if(e instanceof InternalAuthenticationServiceException)
				this.prepareResponseForException(
					httpServletResponse,
					rainTree.failureResponse(Message.INTERNAL_AUTHENTICATION_SERVICE_FAILURE)
				);
			else if(e instanceof MalformedJwtException)
				this.prepareResponseForException(
					httpServletResponse,
					rainTree.failureResponse(Message.MALFORMED_CREDENTIALS)
				);
			else if(e instanceof LockedException)
				this.prepareResponseForException(
					httpServletResponse,
					rainTree.failureResponse(Message.USER_ACCOUNT_LOCKED)
				);
			else this.prepareResponseForException(
					httpServletResponse,
					rainTree.failureResponse(Message.USERNAME_NOT_FOUND)
				);
		} catch (Exception e) {

			exceptionThrown = true;
			exception = e;

			if(!this.isProduction)
				e.printStackTrace();

			this.prepareResponseForException(httpServletResponse, rainTree.failureResponse());
		} finally {
			if(exceptionThrown)
				this.cron.scheduleExceptionLogTask(
					exception,
					exception.getMessage(),
					ReporterID.prepareID(
						this.getClass().getName(),
						methodName
					)
				);
		}
	}

	/**
	 * Writes a {@code 401 Unauthorized} JSON response with the given message body.
	 *
	 * @param response the {@link HttpServletResponse} to write to; must not be {@code null}.
	 * @param message  the JSON string produced by {@link RainTree} to use as the response body.
	 * @throws IOException if writing to the response output stream fails.
	 */
	private void prepareResponseForException(@NotNull HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(Constant.RESPONSE_TYPE_APPLICATION_JSON);
		response.setCharacterEncoding(Constant.ENCODING_UTF_8);
		response.getWriter().write(message);
	}
}

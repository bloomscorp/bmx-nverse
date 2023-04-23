package com.bloomscorp.nverse;

import com.bloomscorp.alfred.cron.CronManager;
import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.Log;
import com.bloomscorp.alfred.support.ReporterID;
import com.bloomscorp.hastar.AuthorizationException;
import com.bloomscorp.hastar.CarrierException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@AllArgsConstructor
public class NVerseExceptionHandlerFilter<
	L extends Log,
	A extends AuthenticationLog,
	T extends NVerseTenant<E>,
	E extends Enum<E>
> extends OncePerRequestFilter {

	private final RainTree rainTree;
	private final CronManager<L, A, T, E> cron;
	private final boolean isProduction;

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

	private void prepareResponseForException(@NotNull HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(Constant.RESPONSE_TYPE_APPLICATION_JSON);
		response.setCharacterEncoding(Constant.ENCODING_UTF_8);
		response.getWriter().write(message);
	}
}

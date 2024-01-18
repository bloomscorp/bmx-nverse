package com.bloomscorp.nverse.validator;

import com.bloomscorp.hastar.code.ActionCode;
import com.bloomscorp.nverse.NVerseHttpRequestWrapper;
import com.bloomscorp.pastebox.Pastebox;
import com.bloomscorp.raintree.RainTree;
import com.bloomscorp.raintree.RainTreeResponse;
import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

@Aspect
@AllArgsConstructor
public class NVerseValidationAspect {

	private static final String _CUSTOM_VALIDATOR_REQUIRED_FIELDS = "headerValues cannot be empty " +
		"while providing headerKeys while using @NVerseDomainValidated annotation!";

	private static final String _UNEQUAL_HEADER_KEY_VALUES = "headerKeys and headerValues should " +
		"be of equal length while using @NVerseDomainValidated annotation";

	private final DomainValidator domainValidator;
	private final RainTree rainTree;

	private Object returnFailedResponse(Class<?> returnType) {
		if (returnType == RainTreeResponse.class)
			return new RainTreeResponse(
				false,
				ActionCode.message(
					ActionCode.UNAUTHORIZED_ACCESS
				)
			);
		else return this.rainTree.failureResponse(ActionCode.UNAUTHORIZED_ACCESS);
	}

	private boolean validateCustomHeaders(
		@NotNull NVerseHttpRequestWrapper request,
		String @NotNull [] headerKeys,
		String[] headerValues
	) {

		for (int i = 0; i < headerKeys.length; i++) {

			String key = request.getHeader(headerKeys[i]);
			String value = headerValues[i];

			if (Pastebox.isEmptyString(key) || Pastebox.isEmptyString(value) || !key.equals(value))
				return false;
		}

		return true;
	}

	private Object validateByDomainValidator(
		@NotNull ProceedingJoinPoint joinPoint,
		NVerseHttpRequestWrapper request,
		Class<?> returnType
	) throws Throwable {
		if (!this.domainValidator.validate(request))
			return this.returnFailedResponse(returnType);
		return joinPoint.proceed();
	}

	private Object validateByCustomHeaders(
		@NotNull ProceedingJoinPoint joinPoint,
		NVerseHttpRequestWrapper request,
		Class<?> returnType,
		String[] headerKeys,
		String @NotNull [] headerValues,
		String origin
	) throws Throwable {

		if (headerValues.length == 0)
			throw new RuntimeException(_CUSTOM_VALIDATOR_REQUIRED_FIELDS);

		if (headerKeys.length != headerValues.length)
			throw new RuntimeException(_UNEQUAL_HEADER_KEY_VALUES);

		if (!this.validateCustomHeaders(request, headerKeys, headerValues))
			return this.returnFailedResponse(returnType);

		if (!origin.isEmpty())
			return this.validateByDomainValidator(
				joinPoint,
				request,
				returnType
			);

		return joinPoint.proceed();
	}

	@Around(
		value = "(@within(NVerseDomainValidated) || "	+
			"@annotation(NVerseDomainValidated)) && "	+
			"args(request, ..)"
	)
	public Object validateDomainContext(
		@NotNull ProceedingJoinPoint joinPoint,
		NVerseHttpRequestWrapper request
	) throws Throwable {

		Signature signature = joinPoint.getSignature();
		Class<?> targetClass = joinPoint.getTarget().getClass();
		MethodSignature methodSignature = (MethodSignature) signature;
		Method method = methodSignature.getMethod();
		Class<?> returnType = methodSignature.getReturnType();
//		NVerseDomainValidated domainValidatedAnnotation = method.getAnnotation(NVerseDomainValidated.class);
		NVerseDomainValidated domainValidatedAnnotation = targetClass.getAnnotation(NVerseDomainValidated.class);

		if (domainValidatedAnnotation == null) {
			throw new RuntimeException("Cannot find @NVerseDomainValidated annotation on the class level!");
		}

		String[] headerKeys = domainValidatedAnnotation.headerKeys();
		String[] headerValues = domainValidatedAnnotation.headerValues();
		String origin = domainValidatedAnnotation.origin();

		if (headerKeys.length == 0)
			return this.validateByDomainValidator(
				joinPoint,
				request,
				returnType
			);

		return this.validateByCustomHeaders(
			joinPoint,
			request,
			returnType,
			headerKeys,
			headerValues,
			origin
		);
	}
}

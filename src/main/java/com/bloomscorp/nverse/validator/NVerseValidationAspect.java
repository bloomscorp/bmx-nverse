package com.bloomscorp.nverse.validator;

import com.bloomscorp.hastar.code.ActionCode;
import com.bloomscorp.nverse.NVerseHttpRequestWrapper;
import com.bloomscorp.raintree.RainTree;
import com.bloomscorp.raintree.RainTreeResponse;
import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jetbrains.annotations.NotNull;

@Aspect
@AllArgsConstructor
public class NVerseValidationAspect {

	private final DomainValidator domainValidator;
	private final RainTree rainTree;

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
		Class<?> returnType = ((MethodSignature) signature).getReturnType();

		if (!this.domainValidator.validate(request)) {
			if (returnType == RainTreeResponse.class)
				return new RainTreeResponse(
					false,
					ActionCode.message(
						ActionCode.UNAUTHORIZED_ACCESS
					)
				);
			else return this.rainTree.failureResponse(ActionCode.UNAUTHORIZED_ACCESS);
		}

		return joinPoint.proceed();
	}
}

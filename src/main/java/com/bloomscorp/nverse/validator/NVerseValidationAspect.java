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

/**
 * AspectJ aspect that intercepts controller methods annotated with {@link NVerseDomainValidated}
 * and enforces domain- and/or custom-header-based request validation before the method executes.
 *
 * <p>This aspect uses an {@code @Around} advice that matches any method or type annotated
 * with {@link NVerseDomainValidated} whose first argument is an
 * {@link NVerseHttpRequestWrapper}. The advice runs the applicable validation path and
 * either proceeds to the original method or short-circuits with a failure response.</p>
 *
 * <p>The validation logic follows this decision tree:</p>
 * <ol>
 *   <li>Read the {@link NVerseDomainValidated} annotation from the target <em>class</em>
 *       (not the method). If absent, a {@link RuntimeException} is thrown.</li>
 *   <li>If {@code headerKeys} is empty → run domain-only validation via
 *       {@link DomainValidator}.</li>
 *   <li>If {@code headerKeys} is non-empty:
 *     <ul>
 *       <li>Require {@code headerValues} to be non-empty and the same length as
 *           {@code headerKeys}; otherwise throw {@link RuntimeException}.</li>
 *       <li>Validate all key-value pairs against the request headers.</li>
 *       <li>If {@code origin} is non-empty, additionally run domain validation after
 *           custom header validation passes.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>When validation fails, the return type of the intercepted method determines the
 * failure response format:</p>
 * <ul>
 *   <li>{@link RainTreeResponse} return type → returns a {@code new RainTreeResponse(false, ...)}.</li>
 *   <li>Any other return type → returns {@link RainTree#failureResponse(int)} with
 *       {@link ActionCode#UNAUTHORIZED_ACCESS}.</li>
 * </ul>
 *
 * @see NVerseDomainValidated
 * @see DomainValidator
 */
@Aspect
@AllArgsConstructor
public class NVerseValidationAspect {

	private static final String _CUSTOM_VALIDATOR_REQUIRED_FIELDS = "headerValues cannot be empty " +
		"while providing headerKeys while using @NVerseDomainValidated annotation!";

	private static final String _UNEQUAL_HEADER_KEY_VALUES = "headerKeys and headerValues should " +
		"be of equal length while using @NVerseDomainValidated annotation";

	private final DomainValidator domainValidator;
	private final RainTree rainTree;

	/**
	 * Returns the appropriate failure response for the given method return type when
	 * validation does not pass.
	 *
	 * @param returnType the declared return type of the intercepted method.
	 * @return a {@link RainTreeResponse} object if the return type is exactly
	 *         {@link RainTreeResponse}; otherwise a JSON failure string produced by
	 *         {@link RainTree#failureResponse(int)}.
	 */
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

	/**
	 * Validates that each header named in {@code headerKeys} is present in the request
	 * and that its value equals the corresponding entry in {@code headerValues}.
	 *
	 * @param request      the wrapped request to inspect; must not be {@code null}.
	 * @param headerKeys   the array of header names to check; must not be {@code null}.
	 * @param headerValues the array of expected header values, indexed parallel to {@code headerKeys}.
	 * @return {@code true} if every key-value pair is present and matches; {@code false} otherwise.
	 */
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

	/**
	 * Runs domain-only validation using {@link DomainValidator}. Proceeds to the join
	 * point if validation passes; returns a failure response otherwise.
	 *
	 * @param joinPoint  the intercepted method join point.
	 * @param request    the wrapped request containing the {@code Origin} header.
	 * @param returnType the declared return type of the intercepted method.
	 * @return the join point's result if domain validation passes, or a failure response.
	 * @throws Throwable if the join point throws.
	 */
	private Object validateByDomainValidator(
		@NotNull ProceedingJoinPoint joinPoint,
		NVerseHttpRequestWrapper request,
		Class<?> returnType
	) throws Throwable {
		if (!this.domainValidator.validate(request))
			return this.returnFailedResponse(returnType);
		return joinPoint.proceed();
	}

	/**
	 * Runs custom header validation and, optionally, domain validation, then either
	 * proceeds to the join point or returns a failure response.
	 *
	 * @param joinPoint    the intercepted method join point.
	 * @param request      the wrapped request to inspect.
	 * @param returnType   the declared return type of the intercepted method.
	 * @param headerKeys   the header names to validate.
	 * @param headerValues the expected header values, indexed parallel to {@code headerKeys};
	 *                     must not be {@code null}.
	 * @param origin       if non-empty, triggers an additional domain validation pass after
	 *                     custom header validation succeeds.
	 * @return the join point's result if all validations pass, or a failure response.
	 * @throws Throwable            if the join point throws.
	 * @throws RuntimeException     if {@code headerValues} is empty when {@code headerKeys}
	 *                              is non-empty, or if the arrays are of unequal length.
	 */
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

	/**
	 * Around advice that intercepts methods (or all methods on types) annotated with
	 * {@link NVerseDomainValidated} whose first argument is an {@link NVerseHttpRequestWrapper}.
	 *
	 * <p>Reads the annotation from the target class, determines the applicable validation
	 * path, and either proceeds or short-circuits with a typed failure response.</p>
	 *
	 * @param joinPoint the intercepted method join point; must not be {@code null}.
	 * @param request   the first argument of the intercepted method, expected to be an
	 *                  {@link NVerseHttpRequestWrapper}.
	 * @return the intercepted method's return value if validation passes, or a failure
	 *         response if it does not.
	 * @throws Throwable        if the intercepted method throws, or if annotation lookup fails.
	 * @throws RuntimeException if {@link NVerseDomainValidated} is not found on the target class.
	 */
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
		Class<?> returnType = methodSignature.getReturnType();
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

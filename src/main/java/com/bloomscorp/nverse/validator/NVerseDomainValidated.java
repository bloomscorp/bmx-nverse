package com.bloomscorp.nverse.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller class or method as requiring domain and/or custom header
 * validation before the handler is invoked.
 *
 * <p>When applied, {@link NVerseValidationAspect} intercepts the annotated method
 * and performs one or both of the following checks:</p>
 *
 * <ul>
 *   <li><strong>Domain validation</strong> — verifies that the {@code Origin} header
 *       of the request is present in the list of allowed domains configured in
 *       {@link DomainValidator}. Triggered when {@code headerKeys} is empty, or when
 *       {@code origin} is non-empty after custom header validation passes.</li>
 *   <li><strong>Custom header validation</strong> — verifies that each header name in
 *       {@code headerKeys} is present in the request and that its value equals the
 *       corresponding entry in {@code headerValues}. All key-value pairs must match.
 *       Triggered when {@code headerKeys} is non-empty.</li>
 * </ul>
 *
 * <p>This annotation must be placed at the <strong>class level</strong> because
 * {@link NVerseValidationAspect} reads the annotation from the target class, not the
 * method. Applying it only at the method level without a class-level annotation will
 * cause the aspect to throw a {@link RuntimeException}.</p>
 *
 * <p>If validation fails, the aspect returns a failure response without invoking the
 * original method — either as a {@link com.bloomscorp.raintree.RainTreeResponse} object
 * or as a {@link com.bloomscorp.raintree.RainTree} failure JSON string, depending on
 * the method's declared return type.</p>
 *
 * @see NVerseValidationAspect
 * @see DomainValidator
 */
@Target({
	ElementType.TYPE,
	ElementType.METHOD
})
@Retention(RetentionPolicy.RUNTIME)
public @interface NVerseDomainValidated {

	/**
	 * Custom HTTP header names whose values must be validated.
	 *
	 * <p>Must be the same length as {@link #headerValues}. An empty array means no
	 * custom header check is performed, and only domain validation applies.</p>
	 *
	 * @return array of header names to check; default is empty.
	 */
	String[] headerKeys() default {};

	/**
	 * Expected values for the corresponding {@link #headerKeys}.
	 *
	 * <p>Must be the same length as {@link #headerKeys} when {@link #headerKeys} is
	 * non-empty; otherwise a {@link RuntimeException} is thrown by the aspect.</p>
	 *
	 * @return array of expected header values; default is empty.
	 */
	String[] headerValues() default {};

	/**
	 * When non-empty, triggers an additional domain validation pass after custom header
	 * validation succeeds. The value is reserved for future use specifying the expected
	 * origin explicitly; currently the domain check always uses {@link DomainValidator}.
	 *
	 * @return an origin string, or empty string to skip the post-header domain check.
	 */
	String origin() default "";
}

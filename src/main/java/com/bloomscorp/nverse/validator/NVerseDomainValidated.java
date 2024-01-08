package com.bloomscorp.nverse.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({
	ElementType.TYPE,
	ElementType.METHOD
})
@Retention(RetentionPolicy.RUNTIME)
public @interface NVerseDomainValidated {
	String[] headerKeys() default {};
	String[] headerValues() default {};
	String origin() default "";
}

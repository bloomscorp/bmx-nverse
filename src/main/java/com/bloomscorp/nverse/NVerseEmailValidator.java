package com.bloomscorp.nverse;

import com.bloomscorp.nverse.esapi.NVerseESAPI;
import org.jetbrains.annotations.NotNull;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.ValidationException;

public class NVerseEmailValidator {

	private static final String ESAPI_VALIDATION_CONTEXT_EMAIL = "EMAIL";
	private static final String ESAPI_VALIDATION_EXPRESSION_TYPE_EMAIL = "Email";
	private static final int EMAIL_MAX_LENGTH = 100;

	public String getValidatedEmail(@NotNull String email) {
		try {
			return NVerseESAPI.validator().getValidInput(
				ESAPI_VALIDATION_CONTEXT_EMAIL,
				email.toLowerCase(),
				ESAPI_VALIDATION_EXPRESSION_TYPE_EMAIL,
				EMAIL_MAX_LENGTH,
				false
			);
		} catch (ValidationException e) {
			return null;
		}
	}
}

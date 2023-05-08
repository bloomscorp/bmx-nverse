package com.bloomscorp.nverse;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class NVerseEmailValidator {

	private static final String ESAPI_VALIDATION_CONTEXT_EMAIL = "EMAIL";
	private static final String ESAPI_VALIDATION_EXPRESSION_TYPE_EMAIL = "Email";
	private static final int EMAIL_MAX_LENGTH = 100;
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$", Pattern.CASE_INSENSITIVE);

	public String getValidatedEmail(@NotNull String email) {
		try {

			return EMAIL_PATTERN.matcher(email).matches() ? email : null;

			// TODO: bring back ESAPI
//			return NVerseESAPI.validator().getValidInput(
//				ESAPI_VALIDATION_CONTEXT_EMAIL,
//				email.toLowerCase(),
//				ESAPI_VALIDATION_EXPRESSION_TYPE_EMAIL,
//				EMAIL_MAX_LENGTH,
//				false
//			);
		} catch (Exception e) {
			return null;
		}
	}
}

package com.bloomscorp.nverse.validator;

import com.bloomscorp.nverse.NVerseHttpRequestWrapper;
import com.bloomscorp.pastebox.Pastebox;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Validates incoming HTTP requests by checking that the {@code Origin} header value
 * is present and listed in the configured set of allowed domains.
 *
 * <p>This validator is used by {@link NVerseValidationAspect} when a controller or
 * method is annotated with {@link NVerseDomainValidated} and no custom header keys are
 * specified (or after custom header validation passes with a non-empty {@code origin}
 * attribute). It serves as a lightweight, server-side enforcement of the allowed-origin
 * policy in addition to CORS, ensuring that requests without a recognized origin are
 * rejected before reaching controller logic.</p>
 *
 * @see NVerseValidationAspect
 * @see NVerseDomainValidated
 */
@AllArgsConstructor
public class DomainValidator implements NVerseValidator<NVerseHttpRequestWrapper> {

	/** The list of allowed origin URLs (e.g., {@code "https://app.example.com"}). */
	private final List<String> allowedDomains;

	/**
	 * Returns {@code true} if the {@code Origin} header of the request is non-empty
	 * and is contained in the configured list of allowed domains.
	 *
	 * @param request the wrapped HTTP request to inspect; must not be {@code null}.
	 * @return {@code true} if the origin is allowed; {@code false} if the header is
	 *         absent, blank, or not in the allowed-domains list.
	 */
	@Override
	public boolean validate(@NotNull NVerseHttpRequestWrapper request) {
		String origin = request.getHeader("origin");
		return !Pastebox.isEmptyString(origin) &&  this.allowedDomains.contains(origin);
	}
}

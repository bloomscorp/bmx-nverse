package com.bloomscorp.nverse;

import com.bloomscorp.nverse.pojo.NVERSE_AUTH_PROVIDER;

/**
 * Immutable data carrier for an authentication provider validation request.
 *
 * <p>This record groups the two inputs required by
 * {@link NVerseAuthenticationService#validateAuthProvider}: the username (plaintext
 * email address) and the {@link NVERSE_AUTH_PROVIDER} that the caller claims was used
 * during registration. Consuming endpoints can accept this record as a request body or
 * construct it programmatically before calling the service.</p>
 *
 * @param username the plaintext email address of the account being validated; must not be {@code null}.
 * @param provider the authentication provider to check against the stored tenant record.
 */
public record NVerseAuthProviderValidationRequest(String username, NVERSE_AUTH_PROVIDER provider) {
}

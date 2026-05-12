package com.bloomscorp.nverse;

/**
 * Immutable data carrier for a successful authentication response containing a JWT.
 *
 * <p>Login endpoints return this record (serialized to JSON) after a successful call to
 * {@link NVerseAuthenticationService#authenticate} and a subsequent call to
 * {@link NVerseJWTService#generateToken}. The client should store the token and include
 * it in subsequent requests as {@code Authorization: Bearer <jwt>}.</p>
 *
 * @param jwt the compact, signed JWT string to be sent to the client.
 */
public record NVerseResponse(String jwt) {
}

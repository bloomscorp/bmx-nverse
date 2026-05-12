package com.bloomscorp.nverse;

/**
 * Immutable data carrier for the result of an authentication provider validation check.
 *
 * <p>Returned by endpoints that invoke
 * {@link NVerseAuthenticationService#validateAuthProvider} to report both the boolean
 * outcome and the integer ordinal of the validated provider, allowing clients to
 * distinguish which provider was matched.</p>
 *
 * @param success  {@code true} if the tenant's stored provider matched the requested provider.
 * @param provider the {@link com.bloomscorp.nverse.pojo.NVERSE_AUTH_PROVIDER} ordinal value
 *                 of the provider that was checked.
 */
public record NVerseProviderResponse(boolean success, int provider) {
}

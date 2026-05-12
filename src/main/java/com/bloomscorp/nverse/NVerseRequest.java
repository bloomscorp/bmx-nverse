package com.bloomscorp.nverse;

/**
 * Immutable data carrier for a basic username/password authentication request.
 *
 * <p>Login endpoints can accept this record as the deserialized request body and pass
 * its fields to {@link NVerseAuthenticationService#authenticate}. The {@code username}
 * field should contain the user's plaintext email address, and {@code password} should
 * contain the user's plaintext password — both will be processed (email encrypted,
 * password peppered and BCrypt-verified) internally by the NVerse authentication stack.</p>
 *
 * @param username the plaintext email address supplied by the user; must not be {@code null}.
 * @param password the plaintext password supplied by the user; must not be {@code null}.
 */
public record NVerseRequest(String username, String password) {
}

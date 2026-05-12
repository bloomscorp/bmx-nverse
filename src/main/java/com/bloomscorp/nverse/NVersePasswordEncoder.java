package com.bloomscorp.nverse;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;

/**
 * Peppered BCrypt password encoder that prepends a secret pepper string to every
 * password before delegating to the standard {@link BCryptPasswordEncoder}.
 *
 * <p>Peppering adds a secret value known only to the application (stored outside the
 * database, e.g., in environment variables or a secrets manager) to the password before
 * hashing. This means that even if the database is fully compromised, an attacker without
 * the pepper cannot verify guesses against the stolen hashes.</p>
 *
 * <p>The pepper is prepended by {@link #season} before both encoding and matching,
 * so the {@code encode} → {@code matches} round-trip remains consistent as long as the
 * same pepper is used throughout the lifetime of a hash.</p>
 *
 * @apiNote Changing the pepper in production will invalidate all existing password hashes.
 *          Pepper rotation requires a coordinated re-hash strategy.
 */
public class NVersePasswordEncoder extends BCryptPasswordEncoder {

	private final String pepper;

	/**
	 * Constructs a {@code NVersePasswordEncoder} with the given BCrypt work factor,
	 * secure random source, and pepper string.
	 *
	 * @param strength     the BCrypt work factor (log rounds); typically between 10 and 14.
	 * @param secureRandom the {@link SecureRandom} instance used for salt generation.
	 * @param pepper       the secret pepper prepended to every raw password before hashing;
	 *                     must not be {@code null}.
	 */
	public NVersePasswordEncoder(int strength, SecureRandom secureRandom, String pepper) {
		super(strength, secureRandom);
		this.pepper = pepper;
	}

	/**
	 * Encodes the given raw password by prepending the pepper and delegating to BCrypt.
	 *
	 * @param rawPassword the plaintext password supplied by the user; must not be {@code null}.
	 * @return the BCrypt hash of {@code pepper + rawPassword}.
	 */
	public String encode(String rawPassword) {
		return super.encode(this.season(rawPassword));
	}

	/**
	 * Verifies that the given raw password, after peppering, matches the stored BCrypt hash.
	 *
	 * @param rawPassword     the plaintext password to test; must not be {@code null}.
	 * @param encodedPassword the BCrypt hash previously produced by {@link #encode}.
	 * @return {@code true} if {@code pepper + rawPassword} matches {@code encodedPassword}.
	 */
	@Override
	public boolean matches(@NotNull CharSequence rawPassword, String encodedPassword) {
		return super.matches(this.season(rawPassword.toString()), encodedPassword);
	}

	/**
	 * Prepends the pepper to the given raw password, producing the value that will be
	 * passed to BCrypt for hashing or verification.
	 *
	 * @param rawPassword the plaintext password; must not be {@code null}.
	 * @return the concatenation of {@code pepper} and {@code rawPassword}.
	 */
	@Contract(pure = true)
	private @NotNull String season(String rawPassword) {
		return this.pepper + rawPassword;
	}
}

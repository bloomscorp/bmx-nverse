package com.bloomscorp.nverse;

import org.springframework.dao.InvalidDataAccessApiUsageException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Provides AES-based email address encoding (encryption) and decoding (decryption)
 * with email format validation applied before encryption.
 *
 * <p>Email addresses in the NVerse model are never stored as plaintext in the database.
 * Instead, {@code NVerseEmailEncoder} encrypts the address via {@link NVerseAES} before
 * persistence, and the encrypted form is used as the lookup key during authentication.
 * This class integrates with {@link NVerseUserDetailsService}, which calls
 * {@link #encode} to convert the plaintext email supplied at login into its encrypted
 * form before querying the database.</p>
 *
 * <p>Construction supplies an {@link NVerseAES} key sequence and digest algorithm.
 * If the {@link NVerseAES} instance cannot be created (e.g., unknown algorithm), the
 * internal AES reference is set to {@code null} and all subsequent encode/decode calls
 * will throw {@code NullPointerException} — callers should treat a {@code null}-AES
 * encoder as a fatal configuration error.</p>
 *
 * @see NVerseAES
 * @see NVerseEmailValidator
 */
public class NVerseEmailEncoder {

	private final NVerseEmailValidator validator;
	private final NVerseAES nVerseAES;

	/**
	 * Constructs an {@code NVerseEmailEncoder} with the given key material and email validator.
	 *
	 * @param encoderKey the passphrase used to derive the AES encryption key; must not be {@code null}.
	 * @param validator  the email format validator applied before encrypting; must not be {@code null}.
	 * @param algorithm  the digest algorithm for AES key derivation (e.g., {@link NVerseAES#SHA512}).
	 */
	public NVerseEmailEncoder(
		String encoderKey,
		NVerseEmailValidator validator,
		String algorithm
	) {

		this.validator = validator;

		NVerseAES localAES;

		try { localAES = new NVerseAES(encoderKey, algorithm); }
		catch(NoSuchAlgorithmException e) { localAES = null; }

		this.nVerseAES = localAES;
	}

	/**
	 * Validates and then AES-encrypts the given plaintext email address.
	 *
	 * <p>The email is first passed through {@link NVerseEmailValidator#getValidatedEmail}. If
	 * validation returns {@code null} (invalid format), the subsequent {@link NVerseAES#encrypt}
	 * call will receive {@code null} and will throw a {@link NullPointerException} — callers
	 * should guard against invalid email input before calling this method.</p>
	 *
	 * @param email the plaintext email address to encrypt.
	 * @return a Base64-encoded AES ciphertext of the validated email.
	 * @throws NoSuchAlgorithmException  if the AES algorithm is unavailable.
	 * @throws NoSuchPaddingException    if PKCS5Padding is unavailable.
	 * @throws InvalidKeyException       if the derived key is invalid.
	 * @throws IllegalBlockSizeException if the input length is invalid for the cipher.
	 * @throws BadPaddingException       if padding is incorrect (should not occur during encryption).
	 */
	public String encode(final String email) throws NoSuchAlgorithmException,
		NoSuchPaddingException,
		InvalidKeyException,
		IllegalBlockSizeException,
		BadPaddingException {
		return this.nVerseAES.encrypt(this.validator.getValidatedEmail(email));
	}

	/**
	 * Decrypts the given Base64-encoded AES ciphertext back to a plaintext email address.
	 *
	 * @param encryptedEmail the Base64-encoded AES ciphertext previously produced by {@link #encode}.
	 * @return the original plaintext email address.
	 * @throws NoSuchAlgorithmException          if the AES algorithm is unavailable.
	 * @throws NoSuchPaddingException            if PKCS5Padding is unavailable.
	 * @throws InvalidKeyException               if the derived key is invalid.
	 * @throws IllegalBlockSizeException         if the ciphertext is not a valid AES block multiple.
	 * @throws BadPaddingException               if the key is wrong or the ciphertext is corrupted.
	 * @throws InvalidDataAccessApiUsageException if the cipher is used incorrectly.
	 * @throws IllegalArgumentException          if the Base64 input is malformed.
	 */
	public String decode(final String encryptedEmail) throws NoSuchAlgorithmException,
		NoSuchPaddingException,
		InvalidKeyException,
		IllegalBlockSizeException,
		BadPaddingException,
		InvalidDataAccessApiUsageException,
		IllegalArgumentException {
		return this.nVerseAES.decrypt(encryptedEmail);
	}

	/**
	 * Tests whether a plaintext email address matches a given encrypted email.
	 *
	 * <p>The raw email is validated and encrypted, then compared to the decrypted form
	 * of {@code encodedEmail}. Any cryptographic or encoding exception during the
	 * comparison is silently caught and returns {@code false}, making this method
	 * safe to call without checked-exception handling.</p>
	 *
	 * @param rawEmail     the plaintext email to test; must not be {@code null}.
	 * @param encodedEmail the Base64-encoded AES ciphertext to compare against.
	 * @return {@code true} if {@code rawEmail} (validated and encrypted) decrypts to
	 *         equal the decrypted form of {@code encodedEmail}; {@code false} otherwise.
	 */
	public boolean matches(final String rawEmail, final String encodedEmail) {
		try {
			return this.validator.getValidatedEmail(rawEmail).equals(this.decode(encodedEmail));
		} catch(
			InvalidKeyException 		|
			NoSuchAlgorithmException	|
			BadPaddingException			|
			IllegalBlockSizeException	|
			NoSuchPaddingException 		|
			IllegalArgumentException	|
			InvalidDataAccessApiUsageException e
		) {
			return false;
		}
	}
}

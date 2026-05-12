package com.bloomscorp.nverse;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Provides symmetric AES/ECB/PKCS5Padding encryption and decryption for string values.
 *
 * <p>The secret key is derived by hashing a caller-supplied key sequence with the given
 * digest algorithm (typically {@link #SHA256} or {@link #SHA512}), then truncating the
 * digest to the first 16 bytes to produce a 128-bit AES key. This approach lets
 * arbitrary-length passphrases be used as key material without further key-stretching.</p>
 *
 * <p>This class is used internally by {@link NVerseEmailEncoder} to encrypt email
 * addresses before they are stored in the database, so that the database never holds
 * plaintext email values.</p>
 *
 * @apiNote ECB mode does not use an IV and is therefore deterministic — the same
 *          plaintext always produces the same ciphertext under the same key. This is
 *          intentional here because the encrypted email must be usable as a lookup key.
 *          Do not use this class for scenarios where ciphertext indistinguishability is
 *          required.
 */
public class NVerseAES {

	/*
	 * For other algorithms, visit:
	 * https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#MessageDigest
	 */

	/** Algorithm name constant for SHA-256 digest, suitable for key derivation. */
	public static final String SHA256 = "SHA-256";

	/** Algorithm name constant for SHA-512 digest, suitable for key derivation. */
	public static final String SHA512 = "SHA-512";

	private final SecretKeySpec secretKey;

	/**
	 * Constructs an {@code NVerseAES} instance by deriving a 128-bit AES key from
	 * the given passphrase using the specified message digest algorithm.
	 *
	 * @param keySequence the passphrase from which the AES key is derived; must not be {@code null}.
	 * @param algorithm   the Java {@link MessageDigest} algorithm name (e.g., {@link #SHA256},
	 *                    {@link #SHA512}).
	 * @throws NoSuchAlgorithmException if the specified digest algorithm is not available in the
	 *                                  current JVM security provider.
	 */
	public NVerseAES(@NotNull String keySequence, String algorithm) throws NoSuchAlgorithmException {
		MessageDigest SHA = MessageDigest.getInstance(algorithm);
		byte[] key = Arrays.copyOf(SHA.digest(keySequence.getBytes(StandardCharsets.UTF_8)), 16);
		this.secretKey = new SecretKeySpec(key, "AES");
	}

	/**
	 * Encrypts the given plaintext string using AES/ECB/PKCS5Padding and returns the
	 * result as a Base64-encoded string.
	 *
	 * @param message the plaintext string to encrypt; must not be {@code null}.
	 * @return a Base64-encoded ciphertext string.
	 * @throws NoSuchAlgorithmException          if the AES algorithm is unavailable.
	 * @throws NoSuchPaddingException            if PKCS5Padding is unavailable.
	 * @throws InvalidKeyException               if the derived key is invalid for AES.
	 * @throws IllegalBlockSizeException         if the plaintext length is not a multiple of the
	 *                                           block size after padding (should not occur with PKCS5Padding).
	 * @throws BadPaddingException               if decryption padding is malformed (not expected during encryption).
	 * @throws InvalidDataAccessApiUsageException if the cipher is used incorrectly.
	 * @throws IllegalArgumentException          if the input cannot be processed.
	 */
	public String encrypt(final @NotNull String message) throws NoSuchAlgorithmException,
		NoSuchPaddingException,
		InvalidKeyException,
		IllegalBlockSizeException,
		BadPaddingException,
		InvalidDataAccessApiUsageException,
		IllegalArgumentException {

		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, this.secretKey);

		return Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * Decrypts the given Base64-encoded ciphertext string using AES/ECB/PKCS5Padding.
	 *
	 * @param encodedMessage the Base64-encoded ciphertext produced by {@link #encrypt}; must not be {@code null}.
	 * @return the original plaintext string.
	 * @throws NoSuchAlgorithmException          if the AES algorithm is unavailable.
	 * @throws NoSuchPaddingException            if PKCS5Padding is unavailable.
	 * @throws InvalidKeyException               if the derived key is invalid for AES.
	 * @throws IllegalBlockSizeException         if the ciphertext length is not a valid multiple of the block size.
	 * @throws BadPaddingException               if the padding is incorrect, indicating a wrong key or corrupted data.
	 * @throws InvalidDataAccessApiUsageException if the cipher is used incorrectly.
	 * @throws IllegalArgumentException          if the Base64 input is malformed.
	 */
	public String decrypt(final @NotNull String encodedMessage) throws NoSuchAlgorithmException,
		NoSuchPaddingException,
		InvalidKeyException,
		IllegalBlockSizeException,
		BadPaddingException,
		InvalidDataAccessApiUsageException,
		IllegalArgumentException {

		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, this.secretKey);

		return new String(cipher.doFinal(Base64.getDecoder().decode(encodedMessage.getBytes(StandardCharsets.UTF_8))));
	}
}

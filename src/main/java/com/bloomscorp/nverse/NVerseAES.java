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

public class NVerseAES {

	/*
	 * For other algorithms, visit:
	 * https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#MessageDigest
	 */

	public static final String SHA256 = "SHA-256";
	public static final String SHA512 = "SHA-512";

	private final SecretKeySpec secretKey;

	public NVerseAES(@NotNull String keySequence, String algorithm) throws NoSuchAlgorithmException {
		MessageDigest SHA = MessageDigest.getInstance(algorithm);
		byte[] key = Arrays.copyOf(SHA.digest(keySequence.getBytes(StandardCharsets.UTF_8)), 16);
		this.secretKey = new SecretKeySpec(key, "AES");
	}

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

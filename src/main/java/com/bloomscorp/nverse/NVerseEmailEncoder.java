package com.bloomscorp.nverse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class NVerseEmailEncoder {

	private final NVerseEmailValidator validator;
	private final NVerseAES nVerseAES;

	public NVerseEmailEncoder(
		String encoderKey,
		NVerseEmailValidator validator
	) {

		this.validator = validator;

		NVerseAES localAES;

		try { localAES = new NVerseAES(encoderKey); }
		catch(NoSuchAlgorithmException e) { localAES = null; }

		this.nVerseAES = localAES;
	}

	public NVerseEmailEncoder(NVerseAES nVerseAES, NVerseEmailValidator validator) {
		this.nVerseAES = nVerseAES;
		this.validator = validator;
	}

	public String encode(final String email) throws NoSuchAlgorithmException,
		NoSuchPaddingException,
		InvalidKeyException,
		IllegalBlockSizeException,
		BadPaddingException {
		return this.nVerseAES.encrypt(this.validator.getValidatedEmail(email));
	}

	public String decode(final String encryptedEmail) throws NoSuchAlgorithmException,
		NoSuchPaddingException,
		InvalidKeyException,
		IllegalBlockSizeException,
		BadPaddingException,
		InvalidDataAccessApiUsageException,
		IllegalArgumentException {
		return this.nVerseAES.decrypt(encryptedEmail);
	}

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

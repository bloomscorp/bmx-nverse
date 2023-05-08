package com.bloomscorp.nverse.test;

import com.bloomscorp.nverse.NVerseAES;
import com.bloomscorp.nverse.NVerseEmailEncoder;
import com.bloomscorp.nverse.NVerseEmailValidator;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TestNVerseEmailEncoder {

	public static void main(String[] args) {
		new TestNVerseEmailEncoder().encode();
	}

	public void encode() {

		String email = "debabrata@bloomscorp.com";
		String key = "fjyimsl3892owdnwsd2w";
		String encodedEmail = "32bOQv/37npPb4xTtqnwwV852guu3+ehHdtCkOcdUHY=";
		NVerseEmailValidator emailValidator = new NVerseEmailValidator();
		NVerseEmailEncoder emailEncoder = new NVerseEmailEncoder(
			key,
			emailValidator,
			NVerseAES.SHA512
		);

		try {
			String encoded = emailEncoder.encode(email);
			System.out.println(encoded);
			System.out.println("matches: " + emailEncoder.decode(encodedEmail));
		} catch (NoSuchAlgorithmException |
				 NoSuchPaddingException |
				 InvalidKeyException |
				 IllegalBlockSizeException |
				 BadPaddingException exception) {
		}
	}
}

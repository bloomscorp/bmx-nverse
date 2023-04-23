package com.bloomscorp.nverse;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;

public class NVersePasswordEncoder extends BCryptPasswordEncoder {

	private final String pepper;

	public NVersePasswordEncoder(int strength, SecureRandom secureRandom, String pepper) {
		super(strength, secureRandom);
		this.pepper = pepper;
	}

	public String encode(String rawPassword) {
		return super.encode(this.season(rawPassword));
	}

	@Override
	public boolean matches(@NotNull CharSequence rawPassword, String encodedPassword) {
		return super.matches(this.season(rawPassword.toString()), encodedPassword);
	}

	@Contract(pure = true)
	private @NotNull String season(String rawPassword) {
		return this.pepper + rawPassword;
	}
}

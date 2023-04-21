package com.bloomscorp.nverse;

import com.bloomscorp.nverse.pojo.NVerseTenant;
import com.bloomscorp.nverse.support.Constant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class NVerseJWTService<
	U extends NVerseUser<T, E>,
	T extends NVerseTenant<E>,
	E extends Enum<E>
> {

	private static final long JWT_TOKEN_VALIDITY = 168 * 60 * 60000;

	private final String secret;

	public NVerseJWTService(String secret) {
		this.secret = secret;
	}

	public String generateToken(@NotNull U user) {
		return this.generateToken(new HashMap<>(), user.getUsername());
	}

	public String getUsernameFromToken(String token) {
		return this.getClaimFromToken(token, Claims::getSubject);
	}

	public boolean validateToken(String token, @NotNull UserDetails userDetails) {
		return (
			this.getUsernameFromToken(token).equals(userDetails.getUsername()) &&
				!this.isTokenExpired(token)
		);
	}

	private Claims getAllClaimsFromToken(String token) {
		return Jwts
			.parserBuilder()
			.setSigningKey(this.getSigningKey())
			.build()
			.parseClaimsJws(token)
			.getBody();
	}

	private <T> T getClaimFromToken(String token, @NotNull Function<Claims, T> claimsResolver) {
		return claimsResolver.apply(this.getAllClaimsFromToken(token));
	}

	private Date getExpirationDateFromToken(String token) {
		return this.getClaimFromToken(token, Claims::getExpiration);
	}

	private boolean isTokenExpired(String token) {
		return this.getExpirationDateFromToken(token).before(new Date());
	}

	@Contract(" -> new")
	private @NotNull Key getSigningKey() {
		return Keys.hmacShaKeyFor(Base64.getDecoder().decode(this.secret));
	}

	private String generateToken(Map<String, Object> claims, String subject) {
		return Jwts
			.builder()
			.setClaims(claims)
			.setSubject(subject)
			.setIssuedAt(new Date(System.currentTimeMillis()))
			.setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
			.signWith(this.getSigningKey(), SignatureAlgorithm.HS512)
			.compact();
	}
}

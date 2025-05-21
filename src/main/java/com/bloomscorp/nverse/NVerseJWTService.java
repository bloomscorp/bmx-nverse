package com.bloomscorp.nverse;

import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@AllArgsConstructor
public class NVerseJWTService<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	private final String secret;
	private final long jwtTokenValidity;

	public String generateToken(@NotNull NVerseUser<T, E, R> user) {
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

	private <X> X getClaimFromToken(String token, @NotNull Function<Claims, X> claimsResolver) {
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
			.setExpiration(new Date(System.currentTimeMillis() + this.jwtTokenValidity))
			.signWith(this.getSigningKey(), SignatureAlgorithm.HS512)
			.compact();
	}
}

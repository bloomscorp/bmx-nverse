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

/**
 * Service for generating, parsing, and validating JWT tokens used in NVerse's
 * stateless authentication model.
 *
 * <p>Tokens are signed with HMAC-SHA-512 using a Base64-encoded secret key. The
 * encrypted email address (the username within the NVerse model) is stored as the
 * JWT subject claim. Token validity is controlled by the {@code jwtTokenValidity}
 * duration provided at construction time (in milliseconds).</p>
 *
 * <p>This service is used by {@link NVerseRequestFilter} to extract and validate
 * tokens on incoming requests, and by login controllers to produce tokens upon
 * successful authentication.</p>
 *
 * @param <T> the concrete tenant type, must implement {@link NVerseTenant}.
 * @param <E> the role enum type.
 * @param <R> the concrete role entity type, must implement {@link NVerseRole}.
 *
 * @apiNote The secret must be a Base64-encoded value of sufficient entropy for HMAC-SHA-512
 *          (at least 64 bytes of raw key material is recommended). Storing the secret in
 *          application properties and never hardcoding it is mandatory.
 */
@AllArgsConstructor
public class NVerseJWTService<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	private final String secret;
	private final long jwtTokenValidity;

	/**
	 * Generates a JWT for the given {@link NVerseUser}, using the user's username
	 * (encrypted email address) as the subject claim and no additional custom claims.
	 *
	 * @param user the authenticated user for whom the token is generated; must not be {@code null}.
	 * @return a compact, signed JWT string.
	 */
	public String generateToken(@NotNull NVerseUser<T, E, R> user) {
		return this.generateToken(new HashMap<>(), user.getUsername());
	}

	/**
	 * Extracts the subject claim (encrypted email address) from the given JWT token.
	 *
	 * @param token the compact JWT string to parse.
	 * @return the subject (username) embedded in the token, or {@code null} if parsing fails.
	 */
	public String getUsernameFromToken(String token) {
		return this.getClaimFromToken(token, Claims::getSubject);
	}

	/**
	 * Validates the given token against the supplied {@link UserDetails}.
	 *
	 * <p>Validation passes only if the subject claim extracted from the token equals the
	 * username from {@code userDetails}, and the token has not yet expired.</p>
	 *
	 * @param token       the compact JWT string to validate.
	 * @param userDetails the {@link UserDetails} object to compare the token subject against;
	 *                    must not be {@code null}.
	 * @return {@code true} if the token is valid for the given user; {@code false} otherwise.
	 */
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

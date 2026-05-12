package com.bloomscorp.nverse;

import com.bloomscorp.hastar.NoUserFoundException;
import com.bloomscorp.nverse.dao.NVerseTenantDAO;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import com.bloomscorp.nverse.support.Constant;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;

/**
 * Spring Security {@link UserDetailsService} implementation for the NVerse tenant model.
 *
 * <p>This service bridges Spring Security's username-based user lookup mechanism with
 * NVerse's encrypted-email identity model. It provides three distinct load methods:</p>
 *
 * <ul>
 *   <li>{@link #loadUserByUsername} — accepts a <em>plaintext</em> email address,
 *       encrypts it via {@link NVerseEmailEncoder}, then delegates to the encrypted
 *       lookup path. Used by Spring Security's internal authentication flow
 *       ({@link org.springframework.security.authentication.AuthenticationManager}).</li>
 *   <li>{@link #loadUserByEncryptedUsername} — accepts an already-encrypted email and
 *       delegates to {@link #loadNVerseUserByEncryptedUsername}. Used by
 *       {@link NVerseRequestFilter} to load a user from the JWT subject claim, which
 *       already contains the encrypted email.</li>
 *   <li>{@link #loadNVerseUserByEncryptedUsername} — the primary lookup implementation.
 *       Queries the database via {@link NVerseTenantDAO#retrieveUserByEmail} and wraps
 *       the result in an {@link NVerseUser}. Throws {@link UsernameNotFoundException}
 *       if no matching tenant is found.</li>
 * </ul>
 *
 * @param <T> the concrete tenant type, must implement {@link NVerseTenant}.
 * @param <E> the role enum type.
 * @param <R> the concrete role entity type, must implement {@link NVerseRole}.
 *
 * @see NVerseEmailEncoder
 * @see NVerseTenantDAO
 */
@AllArgsConstructor
public class NVerseUserDetailsService<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> implements UserDetailsService {

	private final NVerseEmailEncoder emailEncoder;
	private final NVerseTenantDAO<T, E, R> tenantDAO;

	/**
	 * Loads a {@link NVerseUser} by encrypting the given plaintext email address and
	 * performing a database lookup using the encrypted form.
	 *
	 * <p>This method is called by Spring Security's authentication infrastructure during
	 * login. Any exception thrown during encryption or lookup is wrapped in a
	 * {@link UsernameNotFoundException} so that Spring Security can handle it uniformly.</p>
	 *
	 * @param username the plaintext email address supplied by the user attempting to log in.
	 * @return the {@link NVerseUser} wrapping the matching tenant entity.
	 * @throws UsernameNotFoundException if email encryption fails or no tenant matches
	 *                                   the encrypted email.
	 */
	@Override
	public NVerseUser<T, E, R> loadUserByUsername(String username) throws UsernameNotFoundException {
		// TODO: handle UsernameNotFoundException. Where is it thrown?
		try {
			return this.loadUserByEncryptedUsername(this.emailEncoder.encode(username));
		} catch (Exception e) {
			// TODO: (maybe?) use CRON job here to log separate exceptions such as NoUserFoundException or other encryption exceptions
			throw new UsernameNotFoundException(username);
		}
	}

	/**
	 * Loads a {@link NVerseUser} directly from the database using an already-encrypted
	 * email address as the lookup key.
	 *
	 * <p>This method is the primary database query path. It is called from both
	 * {@link #loadUserByUsername} (after plaintext encryption) and
	 * {@link NVerseAuthorityResolver} (when resolving a user from an Authorization header).
	 * If the database returns {@code null}, a {@link NoUserFoundException} is thrown, which
	 * is caught internally and rethrown as a {@link UsernameNotFoundException}.</p>
	 *
	 * @param username the AES-encrypted email address to look up in the database.
	 * @return the {@link NVerseUser} wrapping the matching tenant entity.
	 * @throws UsernameNotFoundException if no tenant is found for the given encrypted email,
	 *                                   or if any exception occurs during the lookup.
	 */
	public NVerseUser<T, E, R> loadNVerseUserByEncryptedUsername(
		String username
	) throws UsernameNotFoundException {
		// TODO what happens when one valid user uses token of another valid user
		// TODO: handle UsernameNotFoundException. Where is it thrown? Handle in GlobalExceptionHandler?
		try {

			T user = this.tenantDAO.retrieveUserByEmail(username);

			if (user == null)
				throw new NoUserFoundException(username);

			// TODO: check if this is required during authentication
//			try {
//				user.setDecryptedEmail(emailEncoder.decode(username));
//			} catch(NoSuchAlgorithmException |
//					NoSuchPaddingException |
//					InvalidKeyException |
//					IllegalBlockSizeException |
//					BadPaddingException |
//					IllegalArgumentException |
//					InvalidDataAccessApiUsageException exception) {
//				throw new CarrierException(
//					exception,
//					ErrorCode.EMAIL_ENCODER
//				);
//			}

			return new NVerseUser<>(user);
		} catch (Exception e) {
			throw new UsernameNotFoundException(username);
		}
	}

	/**
	 * Alias for {@link #loadNVerseUserByEncryptedUsername}, provided for use by
	 * {@link NVerseRequestFilter} and other internal consumers that have already
	 * obtained the encrypted username from a JWT token.
	 *
	 * @param username the AES-encrypted email address to look up.
	 * @return the {@link NVerseUser} wrapping the matching tenant entity.
	 * @throws UsernameNotFoundException if no tenant is found or an error occurs.
	 */
	public NVerseUser<T, E, R> loadUserByEncryptedUsername(String username) throws UsernameNotFoundException {
		return this.loadNVerseUserByEncryptedUsername(username);
	}

	/**
	 * Produces a fully-invalidated {@link UserDetails} for the given username.
	 *
	 * <p>All account flags ({@code enabled}, {@code accountNonExpired},
	 * {@code credentialsNonExpired}, {@code accountNonLocked}) are set to {@code false},
	 * with a blank password and no authorities. This is used as a safe sentinel value
	 * that Spring Security will always reject.</p>
	 *
	 * @param username the username to associate with the invalidated details object.
	 * @return a new {@link UserDetails} instance that fails all Spring Security checks.
	 */
	@Contract("_ -> new")
	private @NotNull UserDetails invalidateAuthenticationRequest(String username) {
		return new User(
			username,
			Constant.BLANK_STRING_VALUE,
			false,
			false,
			false,
			false,
			new ArrayList<>()
		);
	}
}

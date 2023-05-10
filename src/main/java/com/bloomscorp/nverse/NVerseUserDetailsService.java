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

@AllArgsConstructor
public class NVerseUserDetailsService<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> implements UserDetailsService {

	private final NVerseEmailEncoder emailEncoder;
	private final NVerseTenantDAO<T, E, R> tenantDAO;

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

	public NVerseUser<T, E, R> loadUserByEncryptedUsername(String username) throws UsernameNotFoundException {
		return this.loadNVerseUserByEncryptedUsername(username);
	}

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

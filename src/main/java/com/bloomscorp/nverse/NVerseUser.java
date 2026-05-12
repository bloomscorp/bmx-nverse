package com.bloomscorp.nverse;

import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.stream.Collectors;

/**
 * Spring Security {@link User} wrapper that bridges the NVerse tenant model and the
 * Spring Security identity model.
 *
 * <p>{@code NVerseUser} maps a {@link NVerseTenant} instance to the properties
 * required by Spring Security's {@link org.springframework.security.core.userdetails.UserDetails}
 * contract:</p>
 * <ul>
 *   <li><strong>username</strong> — the tenant's encrypted email address.</li>
 *   <li><strong>password</strong> — the tenant's BCrypt-encoded peppered password.</li>
 *   <li><strong>enabled</strong> — {@link NVerseTenant#isActive()}.</li>
 *   <li><strong>accountNonExpired</strong> — {@code !}{@link NVerseTenant#isDeleted()};
 *       deleted accounts are treated as expired.</li>
 *   <li><strong>credentialsNonExpired</strong> — {@code !}{@link NVerseTenant#isSuspended()};
 *       suspended accounts are treated as having expired credentials.</li>
 *   <li><strong>accountNonLocked</strong> — always {@code true} currently; to-be-replaced
 *       with a ban check in a future version.</li>
 *   <li><strong>authorities</strong> — each role's enum constant name is mapped to a
 *       {@link SimpleGrantedAuthority}.</li>
 * </ul>
 *
 * <p>The original {@link NVerseTenant} is retained in the {@code tenant} field so that
 * handlers and services can retrieve the full domain object from the Spring Security
 * context without an additional database query.</p>
 *
 * @param <T> the concrete tenant type, must implement {@link NVerseTenant}.
 * @param <E> the role enum type.
 * @param <R> the concrete role entity type, must implement {@link NVerseRole}.
 */
@Getter
@ToString
public class NVerseUser<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> extends User {

	/** The underlying tenant entity this user object wraps. */
	private final T tenant;

	/**
	 * Constructs an {@code NVerseUser} from the given tenant, mapping all account state
	 * flags and roles to the Spring Security {@link User} parent constructor.
	 *
	 * @param tenant the tenant entity to wrap; must not be {@code null}. The tenant's
	 *               {@link NVerseTenant#isActive()}, {@link NVerseTenant#isDeleted()},
	 *               {@link NVerseTenant#isSuspended()}, and {@link NVerseTenant#getRoles()}
	 *               values are read at construction time and fixed for the lifetime of this object.
	 */
	public NVerseUser(@NotNull T tenant) {
		super(
			tenant.getEmail(),
			tenant.getPassword(),
			tenant.isActive(),
			!tenant.isDeleted(),
			!tenant.isSuspended(),
			//TODO: replace accountNonLocked with !tenant.isBanned() here when adding user ban details,
			true,
			tenant
				.getRoles()
				.stream()
				.map(NVerseRole::getRole)
				.map(E::name)
				.map(SimpleGrantedAuthority::new).collect(Collectors.toList())
		);
		this.tenant = tenant;
	}
}

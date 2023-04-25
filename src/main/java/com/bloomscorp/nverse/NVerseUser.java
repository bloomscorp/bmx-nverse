package com.bloomscorp.nverse;

import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.stream.Collectors;

@Getter
@ToString
public class NVerseUser<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> extends User {

	private final T tenant;

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

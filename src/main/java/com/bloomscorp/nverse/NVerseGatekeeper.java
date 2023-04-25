package com.bloomscorp.nverse;

import com.bloomscorp.hastar.code.ActionCode;
import com.bloomscorp.hastar.code.ErrorCode;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

@AllArgsConstructor
public abstract class NVerseGatekeeper<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	private static final String ROLE_GOD_MODE = "ROLE_GOD_MODE";
	private static final String ROLE_SUPER_USER = "ROLE_SUPER_USER";

	private final NVerseSecureLayer<T, E, R> secureLayer;

	private boolean roleGOD(@NotNull T user) {
		return user
			.getRoles()
			.stream()
			.parallel()
			.map(NVerseRole::getRole)
			.anyMatch(e -> e.name().equals(ROLE_GOD_MODE));
	}

	private boolean roleSU(T user) {
		return this.roleGOD(user) ||
			user.getRoles()
				.stream()
				.parallel()
				.map(NVerseRole::getRole)
				.anyMatch(e -> e.name().equals(ROLE_SUPER_USER));
	}

	public NVerseSurveillanceReport runSurveillance(T user, int code) {
		if (!this.secureLayer.initialEmbeddedCheck(user))
			return new NVerseSurveillanceReport(true, ErrorCode.FORGED_REQUEST);
		if (!this.userHasAppropriateAuthority(user, code))
			return new NVerseSurveillanceReport(true, ErrorCode.AUTHORIZATION_DENIED);
		return new NVerseSurveillanceReport(false, ActionCode.NO_ACTION);
	}

	public abstract boolean userHasAppropriateAuthority(T user, int code);
}

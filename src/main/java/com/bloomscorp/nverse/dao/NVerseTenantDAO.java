package com.bloomscorp.nverse.dao;

import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;

public interface NVerseTenantDAO<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {
	T retrieveUserByEmail(String username);
}

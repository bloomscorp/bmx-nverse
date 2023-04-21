package com.bloomscorp.nverse.dao;

import com.bloomscorp.nverse.pojo.NVerseTenant;

public interface NVerseTenantDAO<
	T extends NVerseTenant<E>,
	E extends Enum<E>
> {
	T retrieveUserByEmail(String username);
}

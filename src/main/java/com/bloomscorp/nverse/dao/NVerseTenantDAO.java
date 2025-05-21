package com.bloomscorp.nverse.dao;

import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;

public interface NVerseTenantDAO<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {
	boolean verifyUniqueEmail(String email, boolean encoded);
	boolean verifyUniqueContactNumber(String contactNumber);
	T retrieveUserByEmail(String username);
	int addNewTenant(T user, R userRole);
	int addNewTenant(T user, R userRole, boolean verifyUniqueEmail, boolean verifyUniqueContactNumber);
}

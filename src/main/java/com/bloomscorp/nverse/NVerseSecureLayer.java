package com.bloomscorp.nverse;

import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import com.bloomscorp.pastebox.Pastebox;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NVerseSecureLayer<
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	public boolean initialEmbeddedCheck(T user) {

		if(user == null) return false;

		final long userID = user.getId();
		final String uid = user.getUid();

		return !Pastebox.isEmptyString(uid) && NVerseValidatorUtilities.isGenericNumericIDValid(userID);
	}
}

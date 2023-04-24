package com.bloomscorp.nverse;

import com.bloomscorp.nverse.pojo.NVerseTenant;
import com.bloomscorp.pastebox.Pastebox;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@AllArgsConstructor
public class NVerseSecureLayer<T extends NVerseTenant<E>, E extends Enum<E>> {

	public boolean initialEmbeddedCheck(T user) {

		if(user == null) return false;

		final long userID = user.getId();
		final String valveID = user.getUid();

		return !Pastebox.isEmptyString(valveID) && NVerseValidatorUtilities.isGenericNumericIDValid(userID);
	}
}

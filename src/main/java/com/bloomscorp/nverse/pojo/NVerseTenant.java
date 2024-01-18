package com.bloomscorp.nverse.pojo;

import java.util.List;

public interface NVerseTenant<E extends Enum<E>, R extends NVerseRole<E>> {
	Long getId();
	String getUid();
	String getEmail();
	String getPassword();
	boolean isActive();
	boolean isDeleted();
	boolean isSuspended();
	NVERSE_AUTH_PROVIDER getProvider();
	List<R> getRoles();
}

package com.bloomscorp.nverse.pojo;

import java.util.List;

public interface NVerseTenant<E extends Enum<E>> {
	Long getId();
	String getUid();
	String getEmail();
	String getPassword();
	boolean isActive();
	boolean isDeleted();
	boolean isSuspended();
	List<NVerseRole<E>> getRoles();
}

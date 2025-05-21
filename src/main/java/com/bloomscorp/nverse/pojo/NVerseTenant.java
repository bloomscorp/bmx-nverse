package com.bloomscorp.nverse.pojo;

import java.util.List;

public interface NVerseTenant<E extends Enum<E>, R extends NVerseRole<E>> {
	Long getId();
	String getUid();
	void setUid(String uid);
	String getEmail();
	void setEmail(String email);
	String getContactNumber();
	String getPassword();
	void setPassword(String password);
	boolean isActive();
	void setActive(boolean active);
	boolean isDeleted();
	boolean isSuspended();
	NVERSE_AUTH_PROVIDER getProvider();
	List<R> getRoles();
	void setCreationTime(long creationTime);
	void setProfileImageUrl(String profileImageUrl);
}

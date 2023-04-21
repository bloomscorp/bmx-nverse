package com.bloomscorp.nverse.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public interface NVerseTenant<E extends Enum<E>> {
	String getEmail();
	String getPassword();
	boolean isActive();
	boolean isDeleted();
	boolean isSuspended();
	List<NVerseRole<E>> getRoles();
}

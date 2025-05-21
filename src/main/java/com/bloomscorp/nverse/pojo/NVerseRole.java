package com.bloomscorp.nverse.pojo;

public interface NVerseRole<
	E extends Enum<E>
> {
	Long getId();
	E getRole();
}

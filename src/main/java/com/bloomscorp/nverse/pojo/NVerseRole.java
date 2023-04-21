package com.bloomscorp.nverse.pojo;

import lombok.Getter;

public interface NVerseRole<E extends Enum<E>> {
	E getRole();
}

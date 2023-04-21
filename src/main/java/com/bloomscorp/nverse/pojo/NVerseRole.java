package com.bloomscorp.nverse.pojo;

import lombok.Getter;

@Getter
public class NVerseRole<E extends Enum<E>> {
	private E role;
}

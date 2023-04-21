package com.bloomscorp.nverse.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NVerseTenant<E extends Enum<E>> {
	private String email;
	private String password;
	private boolean active = true;
	private boolean deleted = false;
	private boolean suspended = false;
	private List<NVerseRole<E>> roles;
}

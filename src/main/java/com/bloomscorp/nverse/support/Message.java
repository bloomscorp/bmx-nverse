package com.bloomscorp.nverse.support;

public final class Message {

	private Message() {}

	public static final String IO_FAILURE = "An I/O failure was encountered.";
	public static final String SERVLET_FAILURE = "A servlet failed while trying to process your request.";
	public static final String EXPIRED_CREDENTIALS = "The credentials used have expired.";

	public static final String ILLEGAL_ARGUMENTS = "Illegal credentials were used to authenticate.";
	public static final String BAD_CREDENTIALS = "The credentials provided were incorrect.";
	public static final String ACCOUNT_DISABLED = "The associated account is disabled and not accessible.";
	public static final String INTERNAL_AUTHENTICATION_SERVICE_FAILURE = "Our system failed to authenticate the credentials.";
	public static final String MALFORMED_CREDENTIALS = "The credentials have been tampered with.";
	public static final String USERNAME_NOT_FOUND = "The username does not have any associated account.";
	public static final String USER_ACCOUNT_LOCKED = "The user account may be locked. Contact systems administrator.";
}

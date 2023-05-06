package com.bloomscorp.nverse.esapi;

import org.owasp.esapi.Encoder;
import org.owasp.esapi.SecurityConfiguration;
import org.owasp.esapi.util.ObjFactory;

public final class NVerseESAPI {

	private static String securityConfigurationImplName = System.getProperty("org.owasp.esapi.SecurityConfiguration", "org.owasp.esapi.reference.DefaultSecurityConfiguration");

	private static volatile SecurityConfiguration overrideConfig = null;

	/**
	 * @return the current ESAPI SecurityConfiguration being used to manage the security configuration for
	 * ESAPI for this application.
	 */
	public static SecurityConfiguration securityConfiguration() {
		// copy the volatile into a non-volatile to prevent TOCTTOU race condition
		SecurityConfiguration override = overrideConfig;
		if ( override != null ) {
			return override;
		}

		return ObjFactory.make( securityConfigurationImplName, "SecurityConfiguration" );
	}

	/**
	 * The ESAPI Encoder is primarilly used to provide <i>output</i> encoding to
	 * prevent Cross-Site Scripting (XSS).
	 * @return the current ESAPI Encoder object being used to encode and decode data for this application.
	 */
	@SuppressWarnings("deprecation")
	public static Encoder encoder() {
		return ObjFactory.make( securityConfiguration().getEncoderImplementation(), "Encoder" );
	}
}

package com.bloomscorp.nverse;

import com.bloomscorp.nverse.support.Constant;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Factory for creating a pre-configured Spring {@link CorsConfigurationSource} that
 * permits cross-origin requests from a caller-specified list of UI origins.
 *
 * <p>The produced CORS configuration applies to all URL paths ({@code /**}) and allows:
 * <ul>
 *   <li><strong>Origins</strong> — the comma-separated list supplied via {@link #source}.</li>
 *   <li><strong>Credentials</strong> — always allowed ({@code allowCredentials = true}).</li>
 *   <li><strong>Methods</strong> — {@code GET}, {@code PUT}, {@code POST}, {@code DELETE},
 *       {@code OPTIONS}, {@code HEAD}.</li>
 *   <li><strong>Headers</strong> — a broad set covering all standard Spring and CORS-related
 *       headers, including {@code Authorization}, {@code Origin}, {@code Content-Type},
 *       and several {@code Access-Control-*} variants.</li>
 * </ul>
 *
 * <p>This class is intended to be used within a Spring Security
 * {@link org.springframework.security.config.annotation.web.builders.HttpSecurity#cors}
 * configuration block, via {@link NVerseSecurityFilterChain}.</p>
 */
public class NVerseCORSConfigurationSource {

	/**
	 * Builds and returns a {@link CorsConfigurationSource} configured for the given
	 * comma-separated list of allowed UI origins.
	 *
	 * @param uiOrigins a comma-separated string of allowed origin URLs
	 *                  (e.g., {@code "https://app.example.com,https://admin.example.com"});
	 *                  must not be {@code null} or blank.
	 * @return a {@link UrlBasedCorsConfigurationSource} registered for all paths,
	 *         with the configured origins, methods, and headers applied.
	 */
	public CorsConfigurationSource source(@NotNull String uiOrigins) {

		CorsConfiguration configuration = new CorsConfiguration();
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		configuration.setAllowedOrigins(Arrays.asList(uiOrigins.split(",")));
		configuration.setAllowCredentials(true);

		configuration.setAllowedMethods(Arrays.asList(
			Constant.METHOD_GET,
			Constant.METHOD_PUT,
			Constant.METHOD_POST,
			Constant.METHOD_DELETE,
			Constant.METHOD_OPTIONS,
			Constant.METHOD_HEAD
		));

		configuration.setAllowedHeaders(Arrays.asList(
			Constant.HEADER_X_REQUESTED_WITH,
			HttpHeaders.ORIGIN,
			HttpHeaders.CONTENT_TYPE,
			HttpHeaders.ACCEPT,
			HttpHeaders.AUTHORIZATION,
			HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
			HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
			HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
			HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
			HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
			HttpHeaders.ACCESS_CONTROL_MAX_AGE,
			HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
			HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
			HttpHeaders.AGE,
			HttpHeaders.ALLOW,
			Constant.HEADER_ALTERNATES,
			HttpHeaders.CONTENT_RANGE,
			HttpHeaders.CONTENT_DISPOSITION,
			Constant.HEADER_DESCRIPTION
		));

		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}

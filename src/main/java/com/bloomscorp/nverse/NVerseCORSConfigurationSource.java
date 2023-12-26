package com.bloomscorp.nverse;

import com.bloomscorp.nverse.support.Constant;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NVerseCORSConfigurationSource {

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

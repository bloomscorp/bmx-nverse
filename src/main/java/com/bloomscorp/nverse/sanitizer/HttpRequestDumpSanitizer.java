package com.bloomscorp.nverse.sanitizer;

import com.bloomscorp.hastar.support.Message;
import com.bloomscorp.nverse.NVerseHttpRequestWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class HttpRequestDumpSanitizer extends NVerseSanitizer<NVerseHttpRequestWrapper, String> {

	@Override
	public String getSanitized(@NotNull NVerseHttpRequestWrapper request) {

		StringBuilder requestDump = new StringBuilder();

		try {
			String line;
			BufferedReader br = new BufferedReader(
				new InputStreamReader(
					request.getInputStream()
				)
			);
			while((line = br.readLine()) != null) requestDump.append(line).append("\n");
		} catch (IOException e) {
			requestDump = new StringBuilder(Message.EXCEPTION_READING_REQUEST);
		}

		return this.sanitize(requestDump.toString());
	}
}

package com.bloomscorp.nverse.sanitizer;

import com.bloomscorp.hastar.support.Message;
import com.bloomscorp.nverse.NVerseHttpRequestWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Sanitizes the raw HTTP request body dump read from an {@link NVerseHttpRequestWrapper}.
 *
 * <p>This sanitizer reads the full request body line by line from the wrapper's cached
 * input stream, assembles it into a single string, then passes it through the
 * {@link NVerseSanitizer#sanitize} pipeline (canonicalization → XSS stripping →
 * OWASP HTML sanitization). The sanitized string is typically stored as the
 * {@code data_dump} column in a log entry so that request context can be recorded
 * without persisting raw, potentially malicious input into the database.</p>
 *
 * <p>If an {@link IOException} occurs while reading the stream, the result is the
 * {@link Message#EXCEPTION_READING_REQUEST} error string rather than a partial dump,
 * ensuring a safe fallback value is always returned.</p>
 *
 * @see NVerseSanitizer
 * @see NVerseHttpRequestWrapper
 */
public class HttpRequestDumpSanitizer extends NVerseSanitizer<NVerseHttpRequestWrapper, String> {

	/**
	 * Reads and sanitizes the full HTTP request body from the given wrapped request.
	 *
	 * @param request the {@link NVerseHttpRequestWrapper} whose cached body stream is to be
	 *                read and sanitized; must not be {@code null}.
	 * @return the sanitized request body string, or {@link Message#EXCEPTION_READING_REQUEST}
	 *         if an I/O error occurs during reading.
	 */
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

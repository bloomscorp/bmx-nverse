package com.bloomscorp.nverse;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * A non-blocking {@link ServletInputStream} backed by a fully buffered byte array,
 * enabling the HTTP request body to be read multiple times within the same request lifecycle.
 *
 * <p>Standard {@link jakarta.servlet.http.HttpServletRequest#getInputStream} can only be
 * consumed once. {@link NVerseHttpRequestWrapper} copies the original stream into a
 * {@link ByteArrayOutputStream} and then constructs this class from that buffer, so that
 * subsequent reads (e.g., by sanitizers, validators, and the framework's JSON deserializer)
 * all obtain a fresh {@link ByteArrayInputStream} starting at position zero.</p>
 *
 * <p>Async read listeners are not supported — {@link #setReadListener} always throws
 * {@link UnsupportedOperationException}, which is appropriate because NVerse operates
 * in synchronous (blocking) Servlet mode.</p>
 *
 * @see NVerseHttpRequestWrapper
 */
public class NVerseCachedServletInputStream extends ServletInputStream {

    private final ByteArrayInputStream inputStream;

	/**
	 * Constructs a {@code NVerseCachedServletInputStream} from the contents of the given
	 * {@link ByteArrayOutputStream}, which must already contain the full request body bytes.
	 *
	 * @param outputStream the buffer holding the captured request body bytes; must not be {@code null}.
	 */
    public NVerseCachedServletInputStream(@org.jetbrains.annotations.NotNull ByteArrayOutputStream outputStream) {
        this.inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    }

	/**
	 * Reads the next byte from the cached byte array.
	 *
	 * @return the next byte as an unsigned integer in the range {@code 0–255},
	 *         or {@code -1} if the end of the buffer has been reached.
	 */
    @Override
    public int read() {
        return this.inputStream.read();
    }

	/**
	 * Returns {@code true} when all bytes in the cached buffer have been consumed.
	 *
	 * @return {@code true} if no bytes remain; {@code false} otherwise.
	 */
    @Override
    public boolean isFinished() {
        return this.inputStream.available() == 0;
    }

	/**
	 * Always returns {@code true} because the backing byte array is always ready
	 * for synchronous reading.
	 *
	 * @return {@code true} unconditionally.
	 */
    @Override
    public boolean isReady() {
        return true;
    }

	/**
	 * Not supported — NVerse uses synchronous (blocking) I/O only.
	 *
	 * @param readListener ignored.
	 * @throws UnsupportedOperationException always.
	 */
    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("setReadListener not implemented");
    }
}

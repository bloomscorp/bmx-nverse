package com.bloomscorp.nverse;

import com.bloomscorp.nverse.support.Constant;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * An {@link HttpServletRequestWrapper} that caches the HTTP request body so it can
 * be read more than once during the same request lifecycle.
 *
 * <p>The Servlet specification allows the body input stream to be consumed only once.
 * NVerse requires the body to be accessible at multiple points — by sanitizers in the
 * filter chain and again by Spring's argument resolvers when deserializing controller
 * method parameters. This wrapper solves the problem by eagerly copying the original
 * stream into a {@link ByteArrayOutputStream} on the first call to
 * {@link #getInputStream}, then vending a fresh {@link NVerseCachedServletInputStream}
 * backed by that buffer on every subsequent call.</p>
 *
 * <p>The character encoding is fixed to {@link Constant#ENCODING_UTF_8} at construction
 * time to ensure consistent multi-byte character handling across all downstream readers.</p>
 *
 * @see NVerseCachedServletInputStream
 * @see NVerseHttpRequestFilter
 */
public class NVerseHttpRequestWrapper extends HttpServletRequestWrapper {

    private ByteArrayOutputStream cachedBytes;

	/**
	 * Wraps the given {@link HttpServletRequest} and sets its character encoding to UTF-8.
	 *
	 * @param request the original incoming HTTP request; must not be {@code null}.
	 * @throws UnsupportedEncodingException if the UTF-8 charset is not supported by the JVM
	 *                                      (should never occur in practice).
	 */
    public NVerseHttpRequestWrapper(HttpServletRequest request) throws UnsupportedEncodingException {
        super(request);
        request.setCharacterEncoding(Constant.ENCODING_UTF_8);
    }

    private void cacheInputStream() throws IOException {
        IOUtils.copy(
                super.getInputStream(),
                this.cachedBytes = new ByteArrayOutputStream()
        );
    }

	/**
	 * Returns a {@link NVerseCachedServletInputStream} backed by the buffered request body.
	 *
	 * <p>On the first call the original input stream is fully copied into an internal
	 * {@link ByteArrayOutputStream}. All subsequent calls return a new
	 * {@link NVerseCachedServletInputStream} reading from the same buffer, effectively
	 * allowing the request body to be read any number of times.</p>
	 *
	 * @return a replayable {@link ServletInputStream} over the cached request body.
	 * @throws IOException if reading from the original input stream fails on the first call.
	 */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        if(this.cachedBytes == null)
            this.cacheInputStream();
        return new NVerseCachedServletInputStream(this.cachedBytes);
    }
}

package com.bloomscorp.nverse;

import com.bloomscorp.nverse.support.Constant;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class NVerseHttpRequestWrapper extends HttpServletRequestWrapper {

    private ByteArrayOutputStream cachedBytes;

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

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if(this.cachedBytes == null)
            this.cacheInputStream();
        return new NVerseCachedServletInputStream(this.cachedBytes);
    }
}

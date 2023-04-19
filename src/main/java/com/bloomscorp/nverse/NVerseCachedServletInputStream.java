package com.bloomscorp.nverse;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class NVerseCachedServletInputStream extends ServletInputStream {

    private final ByteArrayInputStream inputStream;

    public NVerseCachedServletInputStream(@org.jetbrains.annotations.NotNull ByteArrayOutputStream outputStream) {
        this.inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    }

    @Override
    public int read() {
        return this.inputStream.read();
    }

    @Override
    public boolean isFinished() {
        return this.inputStream.available() == 0;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("setReadListener not implemented");
    }
}

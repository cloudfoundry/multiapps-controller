package com.sap.cloud.lm.sl.cf.persistence.stream;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import org.apache.commons.io.input.CountingInputStream;

public class AnalyzingInputStream extends InputStream {

    private final DecoratedInputStream<DigestInputStream, DecoratedInputStream<CountingInputStream, InputStream>> delegate;

    public AnalyzingInputStream(InputStream stream, MessageDigest digest) {
        this.delegate = new DigestInputStreamAdapter<>(new CountingInputStreamAdapter<>(stream), digest);
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    public MessageDigest getMessageDigest() {
        return delegate.getDecoratorStream()
                       .getMessageDigest();
    }

    public long getByteCount() {
        CountingInputStream countingInputStream = delegate.getStream()
                                                          .getDecoratorStream();
        return countingInputStream.getByteCount();
    }

}

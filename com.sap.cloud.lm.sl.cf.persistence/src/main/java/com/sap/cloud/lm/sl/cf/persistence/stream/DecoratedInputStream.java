package com.sap.cloud.lm.sl.cf.persistence.stream;

import java.io.IOException;
import java.io.InputStream;

public class DecoratedInputStream<D extends InputStream, T extends InputStream> extends InputStream {

    private final D decoratorStream;
    private final T stream;

    public DecoratedInputStream(D delegatingStream, T delegate) {
        this.decoratorStream = delegatingStream;
        this.stream = delegate;
    }

    public D getDecoratorStream() {
        return decoratorStream;
    }

    public T getStream() {
        return stream;
    }

    @Override
    public int read() throws IOException {
        return decoratorStream.read();
    }

}

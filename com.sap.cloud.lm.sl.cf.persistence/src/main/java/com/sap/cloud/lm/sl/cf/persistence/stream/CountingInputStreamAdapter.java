package com.sap.cloud.lm.sl.cf.persistence.stream;

import java.io.InputStream;

import org.apache.commons.io.input.CountingInputStream;

public class CountingInputStreamAdapter<T extends InputStream> extends DecoratedInputStream<CountingInputStream, T> {

    public CountingInputStreamAdapter(T inputStream) {
        super(new CountingInputStream(inputStream), inputStream);
    }

}

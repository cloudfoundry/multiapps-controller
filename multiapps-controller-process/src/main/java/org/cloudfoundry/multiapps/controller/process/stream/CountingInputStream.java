package org.cloudfoundry.multiapps.controller.process.stream;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.input.ProxyInputStream;

public class CountingInputStream extends ProxyInputStream {

    private final AtomicLong bytes;

    public CountingInputStream(InputStream proxy, AtomicLong counterRef) {
        super(proxy);
        bytes = counterRef;
    }

    @Override
    protected void afterRead(int n) {
        if (n > 0) {
            bytes.addAndGet(n);
        }
    }

}

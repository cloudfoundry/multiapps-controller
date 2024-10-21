package org.cloudfoundry.multiapps.controller.process.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;


public class CountingInputStream extends FilterInputStream {
    public long byteCount = 0;

    protected CountingInputStream(InputStream in) {
        super(in);
    }

    @Override
    public synchronized int read() throws IOException {
        int result = super.read();
        if (result != -1) {
            byteCount++;
        }
        return result;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);
        if (bytesRead != -1) {
            byteCount += bytesRead;
        }
        return bytesRead;
    }

    public long getByteCount() {
        return byteCount;
    }
}

package org.cloudfoundry.multiapps.controller.process.stream;

import java.io.InputStream;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.mta.util.LimitedInputStream;

public class DefaultLimitedInputStream extends LimitedInputStream {

    public DefaultLimitedInputStream(InputStream in, long maxSize) {
        super(in, maxSize);
    }

    @Override
    protected void raiseError(long maxSize, long currentSize) {
        throw new ContentException("The size of the application exceeds max size limit \"{0}\". Bytes read: \"{1}\"", maxSize, currentSize);
    }
}

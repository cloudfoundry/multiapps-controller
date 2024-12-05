package org.cloudfoundry.multiapps.controller.process.stream;

import java.io.InputStream;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.mta.Messages;
import org.cloudfoundry.multiapps.mta.util.LimitedInputStream;

public class DefaultLimitedInputStream extends LimitedInputStream {

    private final String fileName;

    public DefaultLimitedInputStream(InputStream in, String fileName, long maxSize) {
        super(in, maxSize);
        this.fileName = fileName;
    }

    @Override
    protected void raiseError(long maxSize, long currentSize) {
        throw new ContentException(Messages.ERROR_SIZE_OF_FILE_EXCEEDS_CONFIGURED_MAX_SIZE_LIMIT, currentSize, fileName, maxSize);
    }
}

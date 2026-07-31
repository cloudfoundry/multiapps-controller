package org.cloudfoundry.multiapps.controller.persistence.monitoring;

import com.google.cloud.storage.StorageException;
import io.netty.handler.timeout.ReadTimeoutException;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

public class UploadTimeoutMatcher {

    private UploadTimeoutMatcher() {

    }

    public static boolean isUploadTimeoutException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        Throwable cause = throwable.getCause();
        while (cause != null) {
            if (cause instanceof SocketTimeoutException || cause instanceof TimeoutException || cause instanceof ReadTimeoutException || (
                cause instanceof StorageException
                    && ((StorageException) cause).getCode() == 504)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}

package org.cloudfoundry.multiapps.controller.web.upload.exception;

import java.util.concurrent.RejectedExecutionException;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;

public class RejectedAsyncUploadJobException extends RejectedExecutionException {

    private final AsyncUploadJobEntry asyncUploadJobEntry;

    public RejectedAsyncUploadJobException(AsyncUploadJobEntry asyncUploadJobEntry) {
        this.asyncUploadJobEntry = asyncUploadJobEntry;
    }

    public AsyncUploadJobEntry getAsyncUploadJobEntry() {
        return asyncUploadJobEntry;
    }
}

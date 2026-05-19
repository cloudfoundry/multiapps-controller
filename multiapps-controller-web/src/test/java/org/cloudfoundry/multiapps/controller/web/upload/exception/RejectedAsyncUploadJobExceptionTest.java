package org.cloudfoundry.multiapps.controller.web.upload.exception;

import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class RejectedAsyncUploadJobExceptionTest {

    @Mock
    private AsyncUploadJobEntry jobEntry;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testGetAsyncUploadJobEntryReturnsConstructorValue() {
        RejectedAsyncUploadJobException e = new RejectedAsyncUploadJobException(jobEntry);

        Assertions.assertSame(jobEntry, e.getAsyncUploadJobEntry());
    }
}

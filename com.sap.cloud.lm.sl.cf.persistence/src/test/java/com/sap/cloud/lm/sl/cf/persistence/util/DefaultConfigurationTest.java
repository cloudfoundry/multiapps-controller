package com.sap.cloud.lm.sl.cf.persistence.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileUploadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;

public class DefaultConfigurationTest {

    private DefaultConfiguration classUnderTest = null;

    @Test
    public void testNoAdditionalProcessingIsDone() {
        classUnderTest = new DefaultConfiguration();
        assertTrue(classUnderTest.getFileDownloadProcessor(mock(FileEntry.class),
                                                           mock(FileContentProcessor.class)) instanceof DefaultFileDownloadProcessor);
        assertTrue(classUnderTest.getFileUploadProcessor() instanceof DefaultFileUploadProcessor);
    }

}

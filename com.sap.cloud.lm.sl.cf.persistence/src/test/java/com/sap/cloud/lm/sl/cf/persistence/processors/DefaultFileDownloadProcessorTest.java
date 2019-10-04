package com.sap.cloud.lm.sl.cf.persistence.processors;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;

public class DefaultFileDownloadProcessorTest {

    @Test
    public void testNoAdditionalProcessingIsDone() throws Exception {
        String space = "testSpace";
        String fileId = "testFileId";

        final byte[] data = "test".getBytes();

        FileContentProcessor fileContentProcessor = is -> {
            byte[] result = new byte[data.length];
            is.read(result, 0, data.length);
            assertEquals(new String(data), new String(result));
        };

        DefaultFileDownloadProcessor classUnderTest = new DefaultFileDownloadProcessor(space, fileId, fileContentProcessor);
        try (ByteArrayInputStream is = new ByteArrayInputStream(data)) {
            classUnderTest.processContent(is);
        }
    }

}

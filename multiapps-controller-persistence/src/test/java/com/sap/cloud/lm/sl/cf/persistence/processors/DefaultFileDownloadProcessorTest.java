package com.sap.cloud.lm.sl.cf.persistence.processors;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;

public class DefaultFileDownloadProcessorTest {

    private DefaultFileDownloadProcessor classUnderTest = null;

    @Test
    public void testNoAdditionalProcessingIsDone() throws Exception {
        String space = "testSpace";
        String fileId = "testFileId";

        final byte[] data = "test".getBytes();

        FileContentProcessor fileContentProcessor = new FileContentProcessor() {

            @Override
            public void processFileContent(InputStream is) throws IOException {
                byte[] result = new byte[data.length];
                is.read(result, 0, data.length);
                assertEquals(new String(data), new String(result));
            }
        };

        classUnderTest = new DefaultFileDownloadProcessor(space, fileId, fileContentProcessor);
        ByteArrayInputStream is = null;
        try {
            is = new ByteArrayInputStream(data);
            classUnderTest.processContent(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}

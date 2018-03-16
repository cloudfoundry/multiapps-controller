package com.sap.cloud.lm.sl.cf.client.util;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.sap.cloud.lm.sl.common.ContentException;

public class InputStreamProducerTest {

    private static final String MTAR = "com.sap.mta.sample-1.2.1-beta.mtar";
    private static final String ENTRY_NAME = "web/web-server.zip";
    private InputStreamProducer inputStreamProducer;

    @Test(expected = ContentException.class)
    public void testGetNextInputStreamExceedsSize() throws IOException {

        InputStream inputStream = this.getClass()
            .getResourceAsStream(MTAR);
        inputStreamProducer = new InputStreamProducer(inputStream, ENTRY_NAME, 128l);
        inputStreamProducer.getNextInputStream();
    }

    @Test
    public void testGetNextInputStream() throws IOException {

        InputStream inputStream = this.getClass()
            .getResourceAsStream(MTAR);
        inputStreamProducer = new InputStreamProducer(inputStream, ENTRY_NAME, 1024 * 1024l);
        inputStreamProducer.getNextInputStream();
    }
}

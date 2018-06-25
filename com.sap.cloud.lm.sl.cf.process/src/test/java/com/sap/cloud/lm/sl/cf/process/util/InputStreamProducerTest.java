package com.sap.cloud.lm.sl.cf.process.util;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class InputStreamProducerTest {

    private static final String MTAR = "com.sap.mta.sample-1.2.1-beta.mtar";
    private static final String ENTRY_NAME = "web/web-server.zip";
    private InputStreamProducer inputStreamProducer;

    @Test
    public void testGetNextInputStream() throws IOException {

        InputStream inputStream = this.getClass()
            .getResourceAsStream(MTAR);
        inputStreamProducer = new InputStreamProducer(inputStream, ENTRY_NAME);
        inputStreamProducer.getNextInputStream();
    }
}

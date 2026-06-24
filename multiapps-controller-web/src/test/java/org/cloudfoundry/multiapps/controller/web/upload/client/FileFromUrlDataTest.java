package org.cloudfoundry.multiapps.controller.web.upload.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FileFromUrlDataTest {

    @Test
    void testRecordExposesAllAccessors() {
        InputStream stream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        URI uri = URI.create("https://example.com/foo.mtar");

        FileFromUrlData data = new FileFromUrlData(stream, uri, 1234L);

        Assertions.assertSame(stream, data.fileInputStream());
        Assertions.assertEquals(uri, data.uri());
        Assertions.assertEquals(1234L, data.fileSize());
    }

    @Test
    void testRecordEqualsAndHashCodeFromComponents() {
        InputStream stream = new ByteArrayInputStream(new byte[] { 1 });
        URI uri = URI.create("https://example.com/x");

        FileFromUrlData a = new FileFromUrlData(stream, uri, 10L);
        FileFromUrlData b = new FileFromUrlData(stream, uri, 10L);

        Assertions.assertEquals(a, b);
        Assertions.assertEquals(a.hashCode(), b.hashCode());
    }
}

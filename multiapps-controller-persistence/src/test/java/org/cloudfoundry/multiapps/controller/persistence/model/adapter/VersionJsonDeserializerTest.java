package org.cloudfoundry.multiapps.controller.persistence.model.adapter;

import java.io.IOException;

import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;

class VersionJsonDeserializerTest {

    @Mock
    private JsonParser parser;
    @Mock
    private ObjectCodec codec;
    @Mock
    private DeserializationContext context;

    private VersionJsonDeserializer deserializer;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        deserializer = new VersionJsonDeserializer();
        Mockito.when(parser.getCodec())
               .thenReturn(codec);
    }

    @Test
    void testDeserializeReadsStringAndParsesVersion() throws IOException {
        Mockito.when(codec.readValue(parser, String.class))
               .thenReturn("1.2.3");

        Version result = deserializer.deserialize(parser, context);

        Assertions.assertEquals(Version.parseVersion("1.2.3"), result);
    }

    @Test
    void testDeserializeThrowsForNullValue() throws IOException {
        Mockito.when(codec.readValue(parser, String.class))
               .thenReturn(null);

        Assertions.assertThrows(ParsingException.class, () -> deserializer.deserialize(parser, context));
    }

    @Test
    void testDeserializeThrowsForEmptyValue() throws IOException {
        Mockito.when(codec.readValue(parser, String.class))
               .thenReturn("");

        Assertions.assertThrows(ParsingException.class, () -> deserializer.deserialize(parser, context));
    }

    @Test
    void testDeserializeThrowsForUnparseableValue() throws IOException {
        Mockito.when(codec.readValue(parser, String.class))
               .thenReturn("not-a-version");

        Assertions.assertThrows(ParsingException.class, () -> deserializer.deserialize(parser, context));
    }

    @Test
    void testDeserializePropagatesIOExceptionFromCodec() throws IOException {
        IOException failure = new IOException("read failed");
        Mockito.when(codec.readValue(parser, String.class))
               .thenThrow(failure);

        IOException thrown = Assertions.assertThrows(IOException.class, () -> deserializer.deserialize(parser, context));
        Assertions.assertSame(failure, thrown);
    }
}

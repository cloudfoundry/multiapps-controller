package org.cloudfoundry.multiapps.controller.persistence.model.adapter;

import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.DeserializationContext;

class VersionJsonDeserializerTest {

    @Mock
    private JsonParser parser;
    @Mock
    private DeserializationContext context;

    private VersionJsonDeserializer deserializer;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        deserializer = new VersionJsonDeserializer();
    }

    @Test
    void testDeserializeReadsStringAndParsesVersion() {
        Mockito.when(context.readValue(parser, String.class))
               .thenReturn("1.2.3");

        Version result = deserializer.deserialize(parser, context);

        Assertions.assertEquals(Version.parseVersion("1.2.3"), result);
    }

    @Test
    void testDeserializeThrowsForNullValue() {
        Mockito.when(context.readValue(parser, String.class))
               .thenReturn(null);

        Assertions.assertThrows(ParsingException.class, () -> deserializer.deserialize(parser, context));
    }

    @Test
    void testDeserializeThrowsForEmptyValue() {
        Mockito.when(context.readValue(parser, String.class))
               .thenReturn("");

        Assertions.assertThrows(ParsingException.class, () -> deserializer.deserialize(parser, context));
    }

    @Test
    void testDeserializeThrowsForUnparseableValue() {
        Mockito.when(context.readValue(parser, String.class))
               .thenReturn("not-a-version");

        Assertions.assertThrows(ParsingException.class, () -> deserializer.deserialize(parser, context));
    }

    @Test
    void testDeserializePropagatesExceptionFromContext() {
        JacksonException failure = new StreamReadException("read failed");
        Mockito.when(context.readValue(parser, String.class))
               .thenThrow(failure);

        JacksonException thrown = Assertions.assertThrows(JacksonException.class, () -> deserializer.deserialize(parser, context));
        Assertions.assertSame(failure, thrown);
    }
}

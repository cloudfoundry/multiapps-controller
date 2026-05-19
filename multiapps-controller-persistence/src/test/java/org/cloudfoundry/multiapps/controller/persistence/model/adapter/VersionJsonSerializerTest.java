package org.cloudfoundry.multiapps.controller.persistence.model.adapter;

import java.io.IOException;

import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

class VersionJsonSerializerTest {

    @Mock
    private JsonGenerator generator;
    @Mock
    private SerializerProvider serializerProvider;

    private VersionJsonSerializer serializer;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        serializer = new VersionJsonSerializer();
    }

    @Test
    void testSerializeWritesStringForNonNullVersion() throws IOException {
        Version version = Version.parseVersion("1.2.3");

        serializer.serialize(version, generator, serializerProvider);

        Mockito.verify(generator)
               .writeString(version.toString());
    }

    @Test
    void testSerializeWritesNullForNullVersion() throws IOException {
        serializer.serialize(null, generator, serializerProvider);

        Mockito.verify(generator)
               .writeNull();
    }
}

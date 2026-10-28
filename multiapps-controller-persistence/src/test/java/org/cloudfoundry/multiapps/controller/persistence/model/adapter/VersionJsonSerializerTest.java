package org.cloudfoundry.multiapps.controller.persistence.model.adapter;

import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

class VersionJsonSerializerTest {

    @Mock
    private JsonGenerator generator;
    @Mock
    private SerializationContext serializationContext;

    private VersionJsonSerializer serializer;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        serializer = new VersionJsonSerializer();
    }

    @Test
    void testSerializeWritesStringForNonNullVersion() {
        Version version = Version.parseVersion("1.2.3");

        serializer.serialize(version, generator, serializationContext);

        Mockito.verify(generator)
               .writeString(version.toString());
    }

    @Test
    void testSerializeWritesNullForNullVersion() {
        serializer.serialize(null, generator, serializationContext);

        Mockito.verify(generator)
               .writeNull();
    }
}

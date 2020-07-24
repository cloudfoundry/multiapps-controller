package org.cloudfoundry.multiapps.controller.core.model.adapter;

import java.io.IOException;

import org.cloudfoundry.multiapps.mta.model.Version;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class VersionJsonDeserializer extends StdDeserializer<Version> {

    private static final long serialVersionUID = 1L;

    public VersionJsonDeserializer() {
        super(Version.class);
    }

    @Override
    public Version deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getCodec()
                             .readValue(parser, String.class);
        return Version.parseVersion(value);
    }

}

package org.cloudfoundry.multiapps.controller.persistence.model.adapter;

import org.cloudfoundry.multiapps.mta.model.Version;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class VersionJsonDeserializer extends StdDeserializer<Version> {

    private static final long serialVersionUID = 1L;

    public VersionJsonDeserializer() {
        super(Version.class);
    }

    @Override
    public Version deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        String value = context.readValue(parser, String.class);
        return Version.parseVersion(value);
    }

}

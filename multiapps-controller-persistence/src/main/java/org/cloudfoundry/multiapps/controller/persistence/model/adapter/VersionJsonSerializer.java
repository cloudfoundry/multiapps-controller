package org.cloudfoundry.multiapps.controller.persistence.model.adapter;

import org.cloudfoundry.multiapps.mta.model.Version;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class VersionJsonSerializer extends StdSerializer<Version> {

    private static final long serialVersionUID = 1L;

    public VersionJsonSerializer() {
        super(Version.class);
    }

    @Override
    public void serialize(Version value, JsonGenerator generator, SerializationContext context) throws JacksonException {
        if (value != null) {
            generator.writeString(value.toString());
        } else {
            generator.writeNull();
        }
    }

}

package org.cloudfoundry.multiapps.controller.persistence.model.adapter;

import java.io.IOException;

import org.cloudfoundry.multiapps.mta.model.Version;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class VersionJsonSerializer extends StdSerializer<Version> {

    private static final long serialVersionUID = 1L;

    public VersionJsonSerializer() {
        super(Version.class);
    }

    @Override
    public void serialize(Version value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        if (value != null) {
            generator.writeString(value.toString());
        } else {
            generator.writeNull();
        }
    }

}

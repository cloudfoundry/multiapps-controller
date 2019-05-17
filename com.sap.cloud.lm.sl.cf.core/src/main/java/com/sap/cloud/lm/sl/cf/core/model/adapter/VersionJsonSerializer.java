package com.sap.cloud.lm.sl.cf.core.model.adapter;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sap.cloud.lm.sl.mta.model.Version;

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

package org.cloudfoundry.multiapps.controller.api.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ProcessTypeSerializer extends StdSerializer<ProcessType> {

    private static final long serialVersionUID = 1L;

    public ProcessTypeSerializer() {
        super(ProcessType.class);
    }

    @Override
    public void serialize(ProcessType value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        if (value != null) {
            generator.writeString(value.getName());
        } else {
            generator.writeNull();
        }
    }

}

package org.cloudfoundry.multiapps.controller.api.model;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class ProcessTypeSerializer extends StdSerializer<ProcessType> {

    private static final long serialVersionUID = 1L;

    public ProcessTypeSerializer() {
        super(ProcessType.class);
    }

    @Override
    public void serialize(ProcessType value, JsonGenerator generator, SerializationContext context) throws JacksonException {
        if (value != null) {
            generator.writeString(value.getName());
        } else {
            generator.writeNull();
        }
    }

}

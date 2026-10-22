package org.cloudfoundry.multiapps.controller.api.model;

import java.time.ZonedDateTime;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class ZonedDateTimeSerializer extends StdSerializer<ZonedDateTime> {

    private static final long serialVersionUID = 1L;

    public ZonedDateTimeSerializer() {
        super(ZonedDateTime.class);
    }

    @Override
    public void serialize(ZonedDateTime value, JsonGenerator generator, SerializationContext context) throws JacksonException {
        if (value != null) {
            generator.writeString(Operation.DATE_TIME_FORMATTER.format(value));
        } else {
            generator.writeNull();
        }
    }

}

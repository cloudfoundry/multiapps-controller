package org.cloudfoundry.multiapps.controller.api.model;

import java.time.ZonedDateTime;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class ZonedDateTimeDeserializer extends StdDeserializer<ZonedDateTime> {

    private static final long serialVersionUID = 1L;

    public ZonedDateTimeDeserializer() {
        super(ZonedDateTime.class);
    }

    @Override
    public ZonedDateTime deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        String zonedDateTime = context.readValue(parser, String.class);
        return ZonedDateTime.parse(zonedDateTime, Operation.DATE_TIME_FORMATTER);
    }

}

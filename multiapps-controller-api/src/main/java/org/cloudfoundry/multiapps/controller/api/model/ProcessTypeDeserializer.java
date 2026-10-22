package org.cloudfoundry.multiapps.controller.api.model;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class ProcessTypeDeserializer extends StdDeserializer<ProcessType> {

    private static final long serialVersionUID = 1L;

    public ProcessTypeDeserializer() {
        super(ProcessType.class);
    }

    @Override
    public ProcessType deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        String processType = context.readValue(parser, String.class);
        return ProcessType.fromString(processType);
    }

}

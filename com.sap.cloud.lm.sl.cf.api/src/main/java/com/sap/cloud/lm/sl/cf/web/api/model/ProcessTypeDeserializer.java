package com.sap.cloud.lm.sl.cf.web.api.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ProcessTypeDeserializer extends StdDeserializer<ProcessType> {

    private static final long serialVersionUID = 1L;

    public ProcessTypeDeserializer() {
        super(ProcessType.class);
    }

    @Override
    public ProcessType deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String processType = parser.getCodec()
            .readValue(parser, String.class);
        return ProcessType.fromString(processType);
    }

}

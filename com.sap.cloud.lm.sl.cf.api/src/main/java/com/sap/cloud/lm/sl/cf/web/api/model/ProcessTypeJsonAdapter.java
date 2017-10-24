package com.sap.cloud.lm.sl.cf.web.api.model;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class ProcessTypeJsonAdapter extends TypeAdapter<ProcessType> {

    @Override
    public void write(JsonWriter out, ProcessType type) throws IOException {
        if (type == null) {
            out.nullValue();
        } else {
            out.value(type.toString());
        }
    }

    @Override
    public ProcessType read(JsonReader in) throws IOException {
        if (!in.hasNext()) {
            return null;
        }
        return ProcessType.fromString(in.nextString());
    }
}
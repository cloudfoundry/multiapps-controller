package com.sap.cloud.lm.sl.cf.web.api.model;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class ProcessTypeJsonAdapter extends TypeAdapter<ProcessType> {

    @Override
    public ProcessType read(JsonReader in) throws IOException {
        return ProcessType.fromString(in.nextString());
    }

    @Override
    public void write(JsonWriter out, ProcessType type) throws IOException {
        out.value(type.toString());
    }

}
package com.sap.cloud.lm.sl.cf.core.model.adapter;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sap.cloud.lm.sl.mta.model.Version;

public class VersionJsonAdapter extends TypeAdapter<Version> {

    @Override
    public Version read(JsonReader in) throws IOException {
        return Version.parseVersion(in.nextString());
    }

    @Override
    public void write(JsonWriter out, Version version) throws IOException {
        out.value(version.toString());
    }

}

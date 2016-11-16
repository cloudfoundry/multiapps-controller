package com.sap.cloud.lm.sl.cf.core.model.adapter;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sap.cloud.lm.sl.mta.model.Version;

public class VersionJsonAdapter extends TypeAdapter<Version> {

    @Override
    public void write(JsonWriter out, Version version) throws IOException {
        if (version == null) {
            out.nullValue();
        } else {
            out.value(version.toString());
        }
    }

    @Override
    public Version read(JsonReader in) throws IOException {
        if (!in.hasNext()) {
            return null;
        }
        return Version.parseVersion(in.nextString());
    }

}

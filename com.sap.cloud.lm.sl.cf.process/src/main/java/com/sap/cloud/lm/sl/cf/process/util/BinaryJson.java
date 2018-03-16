package com.sap.cloud.lm.sl.cf.process.util;

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

public class BinaryJson {

    private Gson gson;

    public BinaryJson() {
        this(new Gson());
    }

    public BinaryJson(Gson gson) {
        this.gson = gson;
    }

    public <T> T unmarshal(byte[] binaryJson, Class<T> classOfT) {
        String stringJson = new String(binaryJson, StandardCharsets.UTF_8);
        return gson.fromJson(stringJson, classOfT);
    }

    public <T> T unmarshal(String stringJson, Class<T> classofT) {
        return gson.fromJson(stringJson, classofT);
    }

    public byte[] marshal(Object object) {
        return gson.toJson(object)
            .getBytes(StandardCharsets.UTF_8);
    }

}

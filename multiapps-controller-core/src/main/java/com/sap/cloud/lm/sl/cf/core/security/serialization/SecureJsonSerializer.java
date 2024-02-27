package com.sap.cloud.lm.sl.cf.core.security.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SecureJsonSerializer extends SecureSerializer<JsonElement<com.google.gson.JsonElement>> {

    public SecureJsonSerializer(SecureSerializerConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected JsonElement<com.google.gson.JsonElement> toTree(Object object) {
        return new JsonElement<>("", "", getGson().toJsonTree(object));
    }

    @Override
    protected String toString(JsonElement<com.google.gson.JsonElement> element) {
        return getGson().toJson(element.getGsonElement());
    }

    private Gson getGson() {
        return (configuration.formattedOutputIsEnabled() ? new GsonBuilder().setPrettyPrinting()
                                                                            .create()
            : new Gson());
    }

}

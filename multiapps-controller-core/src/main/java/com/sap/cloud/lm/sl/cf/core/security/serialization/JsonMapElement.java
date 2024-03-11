package com.sap.cloud.lm.sl.cf.core.security.serialization;

import static com.sap.cloud.lm.sl.mta.util.ValidatorUtil.getPrefixedName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class JsonMapElement extends JsonElement<JsonObject> implements MapElement {

    public JsonMapElement(String name, String prefix, JsonObject element) {
        super(name, prefix, element);
    }

    public static JsonObject asGsonMapping(com.google.gson.JsonElement element) {
        return element.getAsJsonObject();
    }

    public static boolean isGsonMapping(com.google.gson.JsonElement element) {
        return element.isJsonObject();
    }

    private Element toJsonElement(Map.Entry<String, com.google.gson.JsonElement> gsonElement) {
        return new JsonElement<>(gsonElement.getKey(), getPrefixedName(prefix, elementName), gsonElement.getValue());
    }

    private com.google.gson.JsonElement toJsonElement(Object element) {
        return new JsonElement<com.google.gson.JsonElement>("", "", new Gson().toJsonTree(element)).getGsonElement();
    }

    @Override
    public Collection<Element> getMembers() {
        ArrayList<Element> members = new ArrayList<>();
        for (Map.Entry<String, com.google.gson.JsonElement> element : gsonElement.entrySet()) {
            members.add(toJsonElement(element));
        }
        return members;
    }

    @Override
    public void remove(String memberName) {
        gsonElement.remove(memberName);
    }

    @Override
    public void add(String memberName, Object memberValue) {
        gsonElement.add(memberName, toJsonElement(memberValue));
    }

}

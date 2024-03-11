package com.sap.cloud.lm.sl.cf.core.security.serialization;

import static com.sap.cloud.lm.sl.mta.util.ValidatorUtil.getPrefixedName;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

public class JsonListElement extends JsonElement<JsonArray> implements ListElement {

    public JsonListElement(String name, String prefix, JsonArray element) {
        super(name, prefix, element);
    }

    public static JsonArray asGsonListing(com.google.gson.JsonElement element) {
        return element.getAsJsonArray();
    }

    public static boolean isGsonListing(com.google.gson.JsonElement element) {
        return element.isJsonArray();
    }

    private JsonElement<com.google.gson.JsonElement> toJsonElement(Integer index, com.google.gson.JsonElement element) {
        return new JsonElement<>(index.toString(), getPrefixedName(prefix, elementName), element);
    }

    @Override
    public Collection<Element> getMembers() {
        ArrayList<Element> members = new ArrayList<>();
        for (int i = 0; i < gsonElement.size(); i++) {
            members.add(toJsonElement(i, gsonElement.get(i)));
        }
        return members;
    }

    @Override
    public void remove(int index) {
        gsonElement.remove(index);
    }

    @Override
    public void add(Object element) {
        gsonElement.add(new Gson().toJsonTree(element));
    }

}

package com.sap.cloud.lm.sl.cf.core.security.serialization;

import static com.sap.cloud.lm.sl.mta.util.ValidatorUtil.getPrefixedName;

public class JsonElement<T extends com.google.gson.JsonElement> implements Element {

    protected String elementName;
    protected T gsonElement;
    protected String prefix;

    public JsonElement(String elementName, String prefix, T gsonElement) {
        this.elementName = elementName;
        this.prefix = prefix;
        this.gsonElement = gsonElement;
    }

    public T getGsonElement() {
        return gsonElement;
    }

    @Override
    public boolean isSimpleElement() {
        return gsonElement.isJsonPrimitive() && gsonElement.getAsJsonPrimitive()
            .isString();
    }

    @Override
    public boolean isMappingElement() {
        return JsonMapElement.isGsonMapping(gsonElement);
    }

    @Override
    public boolean isListingElement() {
        return JsonListElement.isGsonListing(gsonElement);
    }

    @Override
    public MapElement asMappingElement() {
        return new JsonMapElement(elementName, prefix, JsonMapElement.asGsonMapping(gsonElement));
    }

    @Override
    public ListElement asListingElement() {
        return new JsonListElement(elementName, prefix, JsonListElement.asGsonListing(gsonElement));
    }

    @Override
    public String asSimpleElement() {
        return gsonElement.getAsString();
    }

    @Override
    public String getFullName() {
        return getPrefixedName(prefix, elementName);
    }

    @Override
    public String getName() {
        return elementName;
    }

}

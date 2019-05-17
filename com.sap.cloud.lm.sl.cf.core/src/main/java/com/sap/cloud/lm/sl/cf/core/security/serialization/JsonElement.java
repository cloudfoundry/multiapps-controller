package com.sap.cloud.lm.sl.cf.core.security.serialization;

import static com.sap.cloud.lm.sl.mta.util.ValidatorUtil.getPrefixedName;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JsonElement<T> implements Element {

    protected String name;
    protected String prefix;
    protected T object;

    public JsonElement(String name, String prefix, T object) {
        this.name = name;
        this.prefix = prefix;
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    @Override
    public boolean isScalar() {
        return !isMap() && !isList();
    }

    @Override
    public boolean isMap() {
        return object instanceof Map;
    }

    @Override
    public boolean isList() {
        return object instanceof List;
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapElement asMapElement() {
        return new JsonMapElement(name, prefix, (Map<String, Object>) object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ListElement asListElement() {
        return new JsonListElement(name, prefix, (List<Object>) object);
    }

    @Override
    public String asString() {
        return Objects.toString(object, null);
    }

    @Override
    public String getFullName() {
        return getPrefixedName(prefix, name);
    }

    @Override
    public String getName() {
        return name;
    }

}

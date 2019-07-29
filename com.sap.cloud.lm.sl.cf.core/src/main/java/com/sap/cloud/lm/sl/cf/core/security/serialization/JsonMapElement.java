package com.sap.cloud.lm.sl.cf.core.security.serialization;

import static com.sap.cloud.lm.sl.mta.util.ValidatorUtil.getPrefixedName;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonMapElement extends JsonElement<Map<String, Object>> implements MapElement {

    public JsonMapElement(String name, String prefix, Map<String, Object> object) {
        super(name, prefix, object);
    }

    @Override
    public Collection<Element> getMembers() {
        return object.entrySet()
            .stream()
            .map(this::toJsonElement)
            .collect(Collectors.toList());
    }

    private Element toJsonElement(Map.Entry<String, Object> element) {
        return new JsonElement<>(element.getKey(), getPrefixedName(prefix, name), element.getValue());
    }

    @Override
    public void remove(String memberName) {
        object.remove(memberName);
    }

    @Override
    public void add(String memberName, Object memberValue) {
        object.put(memberName, memberValue);
    }

}

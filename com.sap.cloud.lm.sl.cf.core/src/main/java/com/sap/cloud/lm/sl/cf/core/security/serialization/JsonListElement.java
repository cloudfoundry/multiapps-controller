package com.sap.cloud.lm.sl.cf.core.security.serialization;

import static com.sap.cloud.lm.sl.mta.util.ValidatorUtil.getPrefixedName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JsonListElement extends JsonElement<List<Object>> implements ListElement {

    public JsonListElement(String name, String prefix, List<Object> object) {
        super(name, prefix, object);
    }

    @Override
    public Collection<Element> getMembers() {
        List<Element> members = new ArrayList<>();
        for (int i = 0; i < object.size(); i++) {
            members.add(toJsonElement(i, object.get(i)));
        }
        return members;
    }

    private Element toJsonElement(Integer index, Object element) {
        return new JsonElement<>(index.toString(), getPrefixedName(prefix, name), element);
    }

    @Override
    public void remove(int index) {
        object.remove(index);
    }

    @Override
    public void add(Object element) {
        object.add(element);
    }

}

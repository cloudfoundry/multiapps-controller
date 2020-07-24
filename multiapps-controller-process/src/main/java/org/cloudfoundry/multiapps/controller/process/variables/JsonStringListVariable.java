package org.cloudfoundry.multiapps.controller.process.variables;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.immutables.value.Value;

import com.fasterxml.jackson.core.type.TypeReference;

@Value.Immutable
public abstract class JsonStringListVariable<T> implements ListVariable<T, List<T>> {

    public abstract TypeReference<T> getType();

    @Override
    public Serializer<List<T>> getSerializer() {
        return new Serializer<List<T>>() {

            @Override
            public Object serialize(List<T> values) {
                return values.stream()
                             .map(JsonUtil::toJson)
                             .collect(Collectors.toList());
            }

            @SuppressWarnings("unchecked")
            @Override
            public List<T> deserialize(Object serializedValue) {
                List<String> serializedValues = (List<String>) serializedValue;
                return serializedValues.stream()
                                       .map(value -> JsonUtil.fromJson(value, getType()))
                                       .collect(Collectors.toList());
            }

        };
    }

}

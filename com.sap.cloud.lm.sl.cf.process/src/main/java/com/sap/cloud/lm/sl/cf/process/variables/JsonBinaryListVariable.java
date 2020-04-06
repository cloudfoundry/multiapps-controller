package com.sap.cloud.lm.sl.cf.process.variables;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Value.Immutable
public abstract class JsonBinaryListVariable<T> implements ListVariable<T, List<T>> {

    public abstract TypeReference<T> getType();

    @Override
    public Serializer<List<T>> getSerializer() {
        return new Serializer<List<T>>() {

            @Override
            public Object serialize(List<T> values) {
                return values.stream()
                             .map(JsonUtil::toJsonBinary)
                             .collect(Collectors.toList());
            }

            @SuppressWarnings("unchecked")
            @Override
            public List<T> deserialize(Object serializedValue) {
                List<byte[]> serializedValues = (List<byte[]>) serializedValue;
                return serializedValues.stream()
                                       .map(value -> JsonUtil.fromJsonBinary(value, getType()))
                                       .collect(Collectors.toList());
            }

        };
    }

}

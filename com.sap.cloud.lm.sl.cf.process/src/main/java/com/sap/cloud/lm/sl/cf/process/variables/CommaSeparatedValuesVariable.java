package com.sap.cloud.lm.sl.cf.process.variables;

import java.util.Arrays;
import java.util.List;

import org.immutables.value.Value;

@Value.Immutable
public abstract class CommaSeparatedValuesVariable implements ListVariable<String, List<String>> {

    @Override
    public Serializer<List<String>> getSerializer() {
        return new Serializer<List<String>>() {

            @Override
            public Object serialize(List<String> values) {
                return String.join(",", values);
            }

            @Override
            public List<String> deserialize(Object serializedValue) {
                String commaSeparatedValue = (String) serializedValue;
                return Arrays.asList(commaSeparatedValue.split(","));
            }

        };
    }

}

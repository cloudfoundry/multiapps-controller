package org.cloudfoundry.multiapps.controller.process.variables;

import java.util.Arrays;
import java.util.Collections;
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
                return split((String) serializedValue);
            }

            private List<String> split(String serializedValue) {
                return serializedValue.isEmpty() ? Collections.emptyList() : Arrays.asList(serializedValue.split(","));
            }

        };
    }

}

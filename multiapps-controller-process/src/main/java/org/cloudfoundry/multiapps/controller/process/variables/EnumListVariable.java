package org.cloudfoundry.multiapps.controller.process.variables;

import java.util.List;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.immutables.value.Value;

@Value.Immutable
public abstract class EnumListVariable<T extends Enum<T>> implements ListVariable<T, List<T>> {

    public abstract Class<T> getType();

    @Override
    public Serializer<List<T>> getSerializer() {
        return new Serializer<List<T>>() {

            @Override
            public Object serialize(List<T> values) {
                return values.stream()
                             .map(Enum::toString)
                             .collect(Collectors.toList());
            }

            @SuppressWarnings("unchecked")
            @Override
            public List<T> deserialize(Object serializedValue) {
                List<String> serializedValues = (List<String>) serializedValue;
                return serializedValues.stream()
                                       .map(value -> Enum.valueOf(getType(), value))
                                       .collect(Collectors.toList());
            }

            @Override
            public List<T> deserialize(Object serializedValue, VariableContainer container) {
                return deserialize(serializedValue);
            }

        };
    }

}

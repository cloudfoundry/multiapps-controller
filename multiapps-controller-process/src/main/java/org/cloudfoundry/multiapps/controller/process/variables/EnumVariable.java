package org.cloudfoundry.multiapps.controller.process.variables;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.immutables.value.Value;

@Value.Immutable
public abstract class EnumVariable<T extends Enum<T>> implements Variable<T> {

    public abstract Class<T> getType();

    @Override
    public Serializer<T> getSerializer() {
        return new Serializer<T>() {

            @Override
            public Object serialize(T value) {
                return value.toString();
            }

            @Override
            public T deserialize(Object serializedValue) {
                return Enum.valueOf(getType(), (String) serializedValue);
            }

            @Override
            public T deserialize(Object serializedValue, VariableContainer container) {
                return deserialize(serializedValue);
            }

        };
    }

}

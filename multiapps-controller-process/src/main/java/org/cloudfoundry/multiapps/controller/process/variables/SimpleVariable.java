package org.cloudfoundry.multiapps.controller.process.variables;

import org.immutables.value.Value;

@Value.Immutable
public abstract class SimpleVariable<T> implements Variable<T> {

    @Override
    public Serializer<T> getSerializer() {
        return new Serializer<T>() {

            @Override
            public Object serialize(T object) {
                return object;
            }

            @SuppressWarnings("unchecked")
            @Override
            public T deserialize(Object serializedObject) {
                return (T) serializedObject;
            }

        };
    }

}

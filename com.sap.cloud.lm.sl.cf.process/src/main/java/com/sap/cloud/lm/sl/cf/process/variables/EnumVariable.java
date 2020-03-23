package com.sap.cloud.lm.sl.cf.process.variables;

import org.immutables.value.Value;

@Value.Immutable
public abstract class EnumVariable<T extends Enum<T>> implements Variable<T> {

    public abstract Class<T> getType();

    @Override
    public Serializer<T> getSerializer() {
        return new Serializer<T>() {

            @Override
            public Object serialize(T value) {
                if (value == null) {
                    return null;
                }
                return value.toString();
            }

            @Override
            public T deserialize(Object serializedValue) {
                if (serializedValue == null) {
                    return null;
                }
                return Enum.valueOf(getType(), (String) serializedValue);
            }

        };
    }

}

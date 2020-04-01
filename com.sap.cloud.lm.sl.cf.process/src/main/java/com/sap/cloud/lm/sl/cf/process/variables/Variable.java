package com.sap.cloud.lm.sl.cf.process.variables;

import java.lang.reflect.Type;

import org.immutables.value.Value;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.common.Nullable;

@Value.Immutable
public abstract class Variable<T> {

    public static <T> TypeReference<T> typeReference(Class<T> classOfT) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return classOfT;
            }
        };
    }

    public abstract String getName();

    @Nullable
    public abstract TypeReference<T> getType();

    @Value.Default
    public SerializationStrategy getSerializationStrategy() {
        return SerializationStrategy.DIRECT;
    }

    @Value.Derived
    Serializer<T> getSerializer() {
        switch (getSerializationStrategy()) {
            case DIRECT:
                return new DirectSerializer<>();
            case JSON_STRING:
                return new JsonStringSerializer<>(getType());
            case JSON_BINARY:
                return new JsonBinarySerializer<>(getType());
            default:
                throw new IllegalStateException();
        }
    }

    @Nullable
    public abstract T getDefaultValue();

}

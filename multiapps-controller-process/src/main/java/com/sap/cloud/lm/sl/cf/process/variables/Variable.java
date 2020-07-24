package com.sap.cloud.lm.sl.cf.process.variables;

import java.lang.reflect.Type;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;

public interface Variable<T> {

    static <T> TypeReference<T> typeReference(Class<T> classOfT) {
        if (classOfT == null) {
            throw new NullPointerException();
        }
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return classOfT;
            }
        };
    }

    String getName();

    @Nullable
    T getDefaultValue();

    Serializer<T> getSerializer();

}

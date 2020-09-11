package org.cloudfoundry.multiapps.controller.process.variables;

import java.lang.reflect.Type;

import org.cloudfoundry.multiapps.common.Nullable;

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

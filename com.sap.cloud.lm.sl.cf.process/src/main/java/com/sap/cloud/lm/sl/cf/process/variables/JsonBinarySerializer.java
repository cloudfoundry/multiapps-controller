package com.sap.cloud.lm.sl.cf.process.variables;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

class JsonBinarySerializer<T> implements Serializer<T> {

    private final TypeReference<T> typeReference;

    JsonBinarySerializer(TypeReference<T> typeReference) {
        this.typeReference = typeReference;
    }

    @Override
    public Object serialize(T object) {
        if (object == null) {
            return null;
        }
        return JsonUtil.toJsonBinary(object);
    }

    @Override
    public T deserialize(Object serializedObject) {
        if (serializedObject == null) {
            return null;
        }
        return JsonUtil.fromJsonBinary((byte[]) serializedObject, typeReference);
    }

}

package com.sap.cloud.lm.sl.cf.process.variables;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

class JsonStringSerializer<T> implements Serializer<T> {

    private final TypeReference<T> typeReference;

    JsonStringSerializer(TypeReference<T> typeReference) {
        this.typeReference = typeReference;
    }

    @Override
    public Object serialize(T object) {
        if (object == null) {
            return null;
        }
        return JsonUtil.toJson(object);
    }

    @Override
    public T deserialize(Object serializedObject) {
        if (serializedObject == null) {
            return null;
        }
        return JsonUtil.fromJson((String) serializedObject, typeReference);
    }

}

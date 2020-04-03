package com.sap.cloud.lm.sl.cf.process.variables;

import org.immutables.value.Value;

import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Value.Immutable
public abstract class JsonStringVariable<T> implements TypedVariable<T> {

    @Override
    public Serializer<T> getSerializer() {
        return new Serializer<T>() {

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
                return JsonUtil.fromJson((String) serializedObject, getType());
            }

        };
    }

}

package org.cloudfoundry.multiapps.controller.process.variables;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.immutables.value.Value;

import com.fasterxml.jackson.core.type.TypeReference;

@Value.Immutable
public abstract class JsonStringVariable<T> implements Variable<T> {

    public abstract TypeReference<T> getType();

    @Override
    public Serializer<T> getSerializer() {
        return new Serializer<T>() {

            @Override
            public Object serialize(T object) {
                return JsonUtil.toJson(object);
            }

            @Override
            public T deserialize(Object serializedObject) {
                return JsonUtil.fromJson((String) serializedObject, getType());
            }

            @Override
            public T deserialize(Object serializedValue, VariableContainer container) {
                return deserialize(serializedValue);
            }

        };
    }

}

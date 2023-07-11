package org.cloudfoundry.multiapps.controller.process.variables;

import org.flowable.common.engine.api.variable.VariableContainer;

public interface Serializer<T> {

    Object serialize(T value);

    T deserialize(Object serializedValue);

    T deserialize(Object serializedValue, VariableContainer container);

}

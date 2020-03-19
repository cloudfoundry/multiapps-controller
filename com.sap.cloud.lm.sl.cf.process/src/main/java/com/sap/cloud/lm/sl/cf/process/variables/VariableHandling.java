package com.sap.cloud.lm.sl.cf.process.variables;

import org.flowable.common.engine.api.variable.VariableContainer;

public final class VariableHandling {

    private VariableHandling() {
    }

    public static <T> void set(VariableContainer container, Variable<T> variable, T value) {
        Serializer<T> serializer = variable.getSerializer();
        container.setVariable(variable.getName(), serializer.serialize(value));
    }

    public static <T> T get(VariableContainer container, Variable<T> variable) {
        Object serializedValue = container.getVariable(variable.getName());
        if (serializedValue == null) {
            return variable.getDefaultValue();
        }
        Serializer<T> serializer = variable.getSerializer();
        return serializer.deserialize(serializedValue);
    }

}

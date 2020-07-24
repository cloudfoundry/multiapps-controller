package org.cloudfoundry.multiapps.controller.process.variables;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.variable.api.delegate.VariableScope;

public final class VariableHandling {

    private VariableHandling() {
    }

    public static <T> void set(VariableContainer container, Variable<T> variable, T value) {
        if (value == null) {
            container.setVariable(variable.getName(), null);
            return;
        }
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

    public static void remove(VariableScope scope, Variable<?> variable) {
        scope.removeVariable(variable.getName());
    }

}

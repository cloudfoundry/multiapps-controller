package com.sap.cloud.lm.sl.cf.process.variables;

import org.flowable.variable.api.delegate.VariableScope;

public class VariablesHandler {

    private final VariableScope variableScope;

    public VariablesHandler(VariableScope variableScope) {
        this.variableScope = variableScope;
    }

    public <T> T get(Variable<T> variable) {
        Object serializedValue = variableScope.getVariable(variable.getName());
        if (serializedValue == null) {
            return variable.getDefaultValue();
        }
        Serializer<T> serializer = variable.getSerializer();
        return serializer.deserialize(serializedValue);
    }

    public <T> void set(Variable<T> variable, T value) {
        Serializer<T> serializer = variable.getSerializer();
        Object serializedValue = serializer.serialize(value);
        variableScope.setVariable(variable.getName(), serializedValue);
    }

}

package org.cloudfoundry.multiapps.controller.process.variables;

public class WrappedVariable<T> implements Variable<T> {

    private final Variable<T> variableToDelegate;

    private final Serializer<T> wrappedSerializer;

    public WrappedVariable(Variable<T> delegate, Serializer<T> serializer) {
        this.variableToDelegate = delegate;
        this.wrappedSerializer = serializer;
    }

    @Override
    public String getName() {
        return variableToDelegate.getName();
    }

    @Override
    public T getDefaultValue() {
        return variableToDelegate.getDefaultValue();
    }

    @Override
    public Serializer<T> getSerializer() {
        return wrappedSerializer;
    }

}

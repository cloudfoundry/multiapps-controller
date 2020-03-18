package com.sap.cloud.lm.sl.cf.process.variables;

class DirectSerializer<T> implements Serializer<T> {

    @Override
    public Object serialize(T object) {
        return object;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(Object serializedObject) {
        return (T) serializedObject;
    }

}

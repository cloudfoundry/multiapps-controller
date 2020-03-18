package com.sap.cloud.lm.sl.cf.process.variables;

public interface Serializer<T> {

    Object serialize(T value);

    T deserialize(Object serializedValue);

}

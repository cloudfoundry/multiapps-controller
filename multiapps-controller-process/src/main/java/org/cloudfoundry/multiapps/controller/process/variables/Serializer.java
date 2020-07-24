package org.cloudfoundry.multiapps.controller.process.variables;

public interface Serializer<T> {

    Object serialize(T value);

    T deserialize(Object serializedValue);

}

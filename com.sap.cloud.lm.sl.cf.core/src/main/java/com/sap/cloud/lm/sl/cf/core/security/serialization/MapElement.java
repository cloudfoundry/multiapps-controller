package com.sap.cloud.lm.sl.cf.core.security.serialization;

public interface MapElement extends CompositeElement {

    void remove(String memberName);

    void add(String memberName, Object memberValue);

}

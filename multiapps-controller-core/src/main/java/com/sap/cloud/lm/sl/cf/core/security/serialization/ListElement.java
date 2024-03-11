package com.sap.cloud.lm.sl.cf.core.security.serialization;

public interface ListElement extends CompositeElement {

    void remove(int index);

    void add(Object element);
}

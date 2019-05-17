package com.sap.cloud.lm.sl.cf.core.security.serialization;

public interface Element {

    boolean isMap();

    boolean isList();

    boolean isScalar();

    MapElement asMapElement();

    ListElement asListElement();

    String asString();

    String getFullName();

    String getName();

}

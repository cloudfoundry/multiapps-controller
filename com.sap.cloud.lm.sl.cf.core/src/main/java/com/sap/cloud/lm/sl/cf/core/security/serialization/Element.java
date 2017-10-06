package com.sap.cloud.lm.sl.cf.core.security.serialization;

public interface Element {

    boolean isMappingElement();

    boolean isListingElement();

    boolean isSimpleElement();

    MapElement asMappingElement();

    ListElement asListingElement();

    String asSimpleElement();

    String getFullName();

    String getName();
}

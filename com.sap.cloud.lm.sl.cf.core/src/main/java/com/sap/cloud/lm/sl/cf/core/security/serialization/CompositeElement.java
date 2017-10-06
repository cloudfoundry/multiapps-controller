package com.sap.cloud.lm.sl.cf.core.security.serialization;

import java.util.Collection;

public interface CompositeElement extends Element {

    Collection<Element> getMembers();

}

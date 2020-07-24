package org.cloudfoundry.multiapps.controller.core.helpers.expander;

public interface Expander<T, R> {

    R expand(T object);

}

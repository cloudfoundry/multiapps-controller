package com.sap.cloud.lm.sl.cf.core.helpers.expander;

public interface Expander<T, R> {

    R expand(T object);

}

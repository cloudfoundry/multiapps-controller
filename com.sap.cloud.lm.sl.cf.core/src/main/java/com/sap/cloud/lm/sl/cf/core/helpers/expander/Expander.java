package com.sap.cloud.lm.sl.cf.core.helpers.expander;

import com.sap.cloud.lm.sl.common.SLException;

public interface Expander<T, R> {

    R expand(T object) throws SLException;

}

package com.sap.cloud.lm.sl.cf.process.variables;

import com.fasterxml.jackson.core.type.TypeReference;

interface TypedVariable<T> extends Variable<T> {

    TypeReference<T> getType();

}

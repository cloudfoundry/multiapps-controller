package com.sap.cloud.lm.sl.cf.web.api.model.parameters;

/**
 * Validates and converts the raw parameter value to its appropriate type. The result is then used as input for starting a Flowable process.
 * 
 */
public interface ParameterConverter {

    Object convert(Object value);

}

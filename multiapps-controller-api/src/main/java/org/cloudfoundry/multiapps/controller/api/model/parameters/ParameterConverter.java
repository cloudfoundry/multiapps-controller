package org.cloudfoundry.multiapps.controller.api.model.parameters;

/**
 * Validates and converts the raw parameter value to its appropriate type. The result is then used as input for starting a Flowable process.
 * 
 */
public interface ParameterConverter {

    Object convert(Object value);

}

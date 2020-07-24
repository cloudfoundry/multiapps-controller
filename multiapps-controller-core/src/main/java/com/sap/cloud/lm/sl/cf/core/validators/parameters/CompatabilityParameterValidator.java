package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.List;

public interface CompatabilityParameterValidator {

    boolean isCompatible(String parameter);

    String getParameterName();

    List<String> getIncompatibleParameters();
}

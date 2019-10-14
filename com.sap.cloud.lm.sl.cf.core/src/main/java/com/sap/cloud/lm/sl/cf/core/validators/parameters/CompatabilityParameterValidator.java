package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.List;

public interface CompatabilityParameterValidator {

    public boolean isCompatible(String parameter);

    public String getParameterName();

    public List<String> getIncompatibleParameters();
}

package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.List;

public interface CompatibilityParameterValidator {

    boolean isCompatible(String parameter);

    String getParameterName();

    List<String> getIncompatibleParameters();
}

package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.CompatabilityParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.CompatabilityParametersValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.IdleRoutesCompatabilityValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.RoutesCompatabilityValidator;
import com.sap.cloud.lm.sl.mta.model.Module;

public class ModuleParametersCompatabilityValidator extends CompatabilityParametersValidator<Module> {

    private final Module module;

    public ModuleParametersCompatabilityValidator(Module module, UserMessageLogger userMessageLogger) {
        super(userMessageLogger);
        this.module = module;
    }

    @Override
    public Module validate() {
        List<CompatabilityParameterValidator> moduleValidators = getModuleValidators();
        checkParametersCompatability(module.getParameters(), moduleValidators);
        return module;
    }

    private List<CompatabilityParameterValidator> getModuleValidators() {
        return Arrays.asList(new RoutesCompatabilityValidator(), new IdleRoutesCompatabilityValidator());
    }

    private void checkParametersCompatability(Map<String, Object> parameters, List<CompatabilityParameterValidator> moduleValidators) {
        for (CompatabilityParameterValidator validator : moduleValidators) {
            List<String> incompatibleParameters = getIncompatibleParameters(validator, parameters);
            if (!incompatibleParameters.isEmpty()) {
                warnForIncompatibleParameters(validator.getParameterName(), incompatibleParameters);
            }
        }
    }

    private List<String> getIncompatibleParameters(CompatabilityParameterValidator validator, Map<String, Object> parameters) {
        String parameterNameToValidate = validator.getParameterName();
        if (parameters.containsKey(parameterNameToValidate)) {
            return parameters.keySet()
                             .stream()
                             .filter(moduleParameter -> !validator.isCompatible(moduleParameter))
                             .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void warnForIncompatibleParameters(String parameterNameToValidate, List<String> incompatibleParameters) {
        userMessageLogger.warn(Messages.INCOMPATIBLE_PARAMETERS, module.getName(), incompatibleParameters, parameterNameToValidate);
    }

}

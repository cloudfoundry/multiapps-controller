package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
        Map<String, Object> validModuleParameters = validateParameters(module.getParameters(), moduleValidators);
        module.setParameters(validModuleParameters);
        return module;
    }

    private List<CompatabilityParameterValidator> getModuleValidators() {
        return Arrays.asList(new RoutesCompatabilityValidator(), new IdleRoutesCompatabilityValidator());
    }

    private Map<String, Object> validateParameters(Map<String, Object> parameters, List<CompatabilityParameterValidator> moduleValidators) {
        List<String> allIncompatibleParameters = new ArrayList<>();
        for (CompatabilityParameterValidator validator : moduleValidators) {
            List<String> incompatibleParameters = getIncompatibleParameters(validator, parameters);
            allIncompatibleParameters.addAll(incompatibleParameters);
        }

        return parameters.entrySet()
                         .stream()
                         .filter(parameterKeyValue -> !allIncompatibleParameters.contains(parameterKeyValue.getKey()))
                         .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private List<String> getIncompatibleParameters(CompatabilityParameterValidator validator, Map<String, Object> parameters) {
        String parameterNameToValidate = validator.getParameterName();
        List<String> incompatibleParameters = Collections.emptyList();
        if (parameters.containsKey(parameterNameToValidate)) {
            incompatibleParameters = parameters.keySet()
                                               .stream()
                                               .filter(moduleParameter -> !validator.isCompatible(moduleParameter))
                                               .collect(Collectors.toList());
        }

        if (!incompatibleParameters.isEmpty()) {
            warnForIncompatibleParameters(parameterNameToValidate, incompatibleParameters);
        }

        return incompatibleParameters;
    }

    private void warnForIncompatibleParameters(String parameterNameToValidate, List<String> incompatibleParameters) {
        userMessageLogger.warn(Messages.INCOMPATIBLE_PARAMETERS, module.getName(), incompatibleParameters, parameterNameToValidate);
    }

}

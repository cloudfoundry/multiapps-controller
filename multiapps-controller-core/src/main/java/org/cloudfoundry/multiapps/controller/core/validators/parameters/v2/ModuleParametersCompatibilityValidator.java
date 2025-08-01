package org.cloudfoundry.multiapps.controller.core.validators.parameters.v2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.AppFeaturesCompatibilityValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.CompatibilityParameterValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.CompatibilityParametersValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.IdleRoutesCompatibilityValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.RoutesCompatibilityValidator;
import org.cloudfoundry.multiapps.mta.model.Module;

public class ModuleParametersCompatibilityValidator extends CompatibilityParametersValidator<Module> {

    private final Module module;

    public ModuleParametersCompatibilityValidator(Module module, UserMessageLogger userMessageLogger) {
        super(userMessageLogger);
        this.module = module;
    }

    @Override
    public Module validate() {
        List<CompatibilityParameterValidator> moduleValidators = getModuleValidators();
        checkParametersCompatibility(module.getParameters(), moduleValidators);
        return module;
    }

    private List<CompatibilityParameterValidator> getModuleValidators() {
        return Arrays.asList(new RoutesCompatibilityValidator(), new IdleRoutesCompatibilityValidator(),
                             new AppFeaturesCompatibilityValidator(module.getParameters()));
    }

    private void checkParametersCompatibility(Map<String, Object> parameters, List<CompatibilityParameterValidator> moduleValidators) {
        for (CompatibilityParameterValidator validator : moduleValidators) {
            List<String> incompatibleParameters = getIncompatibleParameters(validator, parameters);
            if (!incompatibleParameters.isEmpty()) {
                warnForIncompatibleParameters(validator.getParameterName(), incompatibleParameters);
            }
        }
    }

    private List<String> getIncompatibleParameters(CompatibilityParameterValidator validator, Map<String, Object> parameters) {
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

package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Hook;

public class HookPhasesConfigValidator {

    private static final String PHASE_KEY = "phase";

    public void validate(DeploymentDescriptor descriptor) {
        descriptor.getModules()
                  .stream()
                  .flatMap(module -> module.getHooks().stream())
                  .forEach(this::validateHookHasNoDuplicatePhaseConfigs);
    }

    @SuppressWarnings("unchecked")
    private void validateHookHasNoDuplicatePhaseConfigs(Hook hook) {
        Object phasesConfigValue = hook.getParameters().get(SupportedParameters.PHASES_CONFIG);
        if (phasesConfigValue == null) {
            return;
        }
        if (!(phasesConfigValue instanceof List)) {
            throw new SLException(MessageFormat.format(Messages.INVALID_PHASES_CONFIG_NOT_A_LIST, hook.getName()));
        }
        List<Map<String, String>> phasesConfig = (List<Map<String, String>>) phasesConfigValue;
        Set<String> seenPhases = new HashSet<>();
        for (Map<String, String> phaseConfig : phasesConfig) {
            String phase = phaseConfig.get(PHASE_KEY);
            if (phase != null && !seenPhases.add(phase)) {
                throw new SLException(MessageFormat.format(Messages.DUPLICATE_PHASE_IN_PHASES_CONFIG, phase, hook.getName()));
            }
        }
    }

}

package com.sap.cloud.lm.sl.cf.process.util;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.core.model.HookPhaseProcessType;
import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.core.model.SubprocessPhase;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessContext;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

@Named
public class HooksPhaseBuilder {

    private static final String HOOKS_DELIMITER = ".";
    private static final String DEFAULT_HOOK_ENTITY = "application";
    private final ProcessTypeParser processTypeParser;

    @Inject
    public HooksPhaseBuilder(ProcessTypeParser processTypeParser) {
        this.processTypeParser = processTypeParser;
    }

    public List<HookPhase> buildHookPhases(List<HookPhase> hookPhases, ProcessContext context) {
        return hookPhases.stream()
                         .map(hookPhase -> buildPhase(hookPhase, context))
                         .map(HookPhase::fromString)
                         .collect(Collectors.toList());
    }

    private String buildPhase(HookPhase hookPhase, ProcessContext context) {
        if (HookPhase.getOldPhases().contains(hookPhase)) {
            return hookPhase.getValue();
        }
        String deploymentType = getDeploymentType(context);
        String fullHookPhase = deploymentType + HOOKS_DELIMITER + DEFAULT_HOOK_ENTITY + HOOKS_DELIMITER + hookPhase.getValue();
        String optionalPhaseLocator = getOptionalPhaseLocator(context);
        return fullHookPhase + optionalPhaseLocator;
    }

    private String getDeploymentType(ProcessContext context) {
        if (ProcessType.DEPLOY.equals(processTypeParser.getProcessType(context.getExecution()))) {
            return HookPhaseProcessType.DEPLOY.getType();
        }
        return HookPhaseProcessType.BLUE_GREEN_DEPLOY.getType();
    }

    private String getOptionalPhaseLocator(ProcessContext context) {
        if (ProcessType.DEPLOY.equals(processTypeParser.getProcessType(context.getExecution()))) {
            return HookPhaseProcessType.HookProcessPhase.NONE.getType();
        }
        if (context.getVariable(Variables.SUBPROCESS_PHASE) == SubprocessPhase.BEFORE_APPLICATION_STOP) {
            return HOOKS_DELIMITER + HookPhaseProcessType.HookProcessPhase.IDLE.getType();
        }
        if (context.getVariable(Variables.PHASE) != Phase.AFTER_RESUME
            && context.getVariable(Variables.SUBPROCESS_PHASE) == SubprocessPhase.BEFORE_APPLICATION_START) {
            return HOOKS_DELIMITER + HookPhaseProcessType.HookProcessPhase.IDLE.getType();
        }
        return HOOKS_DELIMITER + HookPhaseProcessType.HookProcessPhase.LIVE.getType();
    }

}

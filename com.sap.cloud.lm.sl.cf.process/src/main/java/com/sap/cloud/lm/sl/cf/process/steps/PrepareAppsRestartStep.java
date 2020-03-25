package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.mta.model.Module;

@Named("prepareAppsRestartStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareAppsRestartStep extends PrepareModulesDeploymentStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        super.executeStep(context);

        context.setVariable(Variables.REBUILD_APP_ENV, true);
        context.setVariable(Variables.SHOULD_UPLOAD_APPLICATION_CONTENT, false);
        context.setVariable(Variables.EXECUTE_ONE_OFF_TASKS, false);
        context.setVariable(Variables.SHOULD_SKIP_SERVICE_REBINDING, true);
        context.setVariable(Variables.USE_IDLE_URIS, false);
        context.setVariable(Variables.DELETE_IDLE_URIS, true);
        context.setVariable(Variables.SKIP_UPDATE_CONFIGURATION_ENTRIES, false);
        context.setVariable(Variables.SKIP_MANAGE_SERVICE_BROKER, false);
        StepsUtil.setIteratedModulesInParallel(context.getExecution(), Collections.emptyList());

        return StepPhase.DONE;
    }

    @Override
    protected List<Module> getModulesToDeploy(DelegateExecution execution) {
        List<Module> allModulesToDeploy = StepsUtil.getAllModulesToDeploy(execution);
        return allModulesToDeploy.stream()
                                 .filter(module -> moduleToDeployHelper.isApplication(module))
                                 .collect(Collectors.toList());
    }

}

package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.model.ModuleToDeploy;
import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("prepareAppsRestartStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareAppsRestartStep extends PrepareModulesDeploymentStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        super.executeStep(execution);

        execution.getContext()
            .setVariable(Constants.REBUILD_APP_ENV, true);
        execution.getContext()
            .setVariable(Constants.SHOULD_UPLOAD_APPLICATION_CONTENT, false);
        execution.getContext()
            .setVariable(Constants.EXECUTE_ONE_OFF_TASKS, false);
        StepsUtil.setUseIdleUris(execution.getContext(), false);
        StepsUtil.setSkipUpdateConfigurationEntries(execution.getContext(), false);

        StepsUtil.setIteratedModulesInParallel(execution.getContext(), Collections.emptyList());

        return StepPhase.DONE;
    }

    @Override
    protected List<ModuleToDeploy> getModulesToDeploy(DelegateExecution context) {
        List<ModuleToDeploy> allModulesToDeploy = StepsUtil.getAllModulesToDeploy(context);
        return allModulesToDeploy.stream()
            .filter(module -> moduleToDeployHelper.isApplication(module))
            .collect(Collectors.toList());
    }

}

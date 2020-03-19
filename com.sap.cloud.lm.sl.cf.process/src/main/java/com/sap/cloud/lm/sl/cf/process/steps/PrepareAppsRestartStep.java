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
import com.sap.cloud.lm.sl.mta.model.Module;

@Named("prepareAppsRestartStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareAppsRestartStep extends PrepareModulesDeploymentStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        super.executeStep(context);

        context.getExecution()
               .setVariable(Constants.REBUILD_APP_ENV, true);
        context.getExecution()
               .setVariable(Constants.SHOULD_UPLOAD_APPLICATION_CONTENT, false);
        context.getExecution()
               .setVariable(Constants.EXECUTE_ONE_OFF_TASKS, false);
        context.getExecution()
               .setVariable(Constants.VAR_SHOULD_SKIP_SERVICE_REBINDING, true);
        StepsUtil.setUseIdleUris(context.getExecution(), false);
        StepsUtil.setDeleteIdleUris(context.getExecution(), true);
        StepsUtil.setSkipUpdateConfigurationEntries(context.getExecution(), false);
        StepsUtil.setSkipManageServiceBroker(context.getExecution(), false);
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

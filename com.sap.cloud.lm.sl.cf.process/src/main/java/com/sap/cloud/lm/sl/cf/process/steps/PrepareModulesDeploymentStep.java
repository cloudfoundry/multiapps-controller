package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.mta.model.Module;

@Named("prepareModulesDeploymentStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareModulesDeploymentStep extends SyncFlowableStep {

    @Inject
    protected ProcessTypeParser processTypeParser;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.PREPARING_MODULES_DEPLOYMENT);

        // Get the list of cloud modules from the context:
        List<Module> modulesToDeploy = getModulesToDeploy(context.getExecution());

        // Initialize the iteration over the applications list:
        context.getExecution()
               .setVariable(Constants.VAR_MODULES_COUNT, modulesToDeploy.size());
        context.getExecution()
               .setVariable(Constants.VAR_MODULES_INDEX, 0);
        context.getExecution()
               .setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_MODULES_INDEX);

        context.getExecution()
               .setVariable(Constants.REBUILD_APP_ENV, true);
        context.getExecution()
               .setVariable(Constants.SHOULD_UPLOAD_APPLICATION_CONTENT, true);
        context.getExecution()
               .setVariable(Constants.EXECUTE_ONE_OFF_TASKS, true);

        StepsUtil.setModulesToDeploy(context.getExecution(), modulesToDeploy);

        ProcessType processType = processTypeParser.getProcessType(context.getExecution());

        StepsUtil.setDeleteIdleUris(context.getExecution(), false);
        StepsUtil.setSkipUpdateConfigurationEntries(context.getExecution(), ProcessType.BLUE_GREEN_DEPLOY.equals(processType));
        StepsUtil.setSkipManageServiceBroker(context.getExecution(), ProcessType.BLUE_GREEN_DEPLOY.equals(processType));
        StepsUtil.setUseIdleUris(context.getExecution(), ProcessType.BLUE_GREEN_DEPLOY.equals(processType));

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PREPARING_MODULES_DEPLOYMENT;
    }

    protected List<Module> getModulesToDeploy(DelegateExecution execution) {
        return StepsUtil.getAllModulesToDeploy(execution);
    }

}

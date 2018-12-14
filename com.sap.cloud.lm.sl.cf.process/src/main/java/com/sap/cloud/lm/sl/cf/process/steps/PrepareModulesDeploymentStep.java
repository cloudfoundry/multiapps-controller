package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.model.ModuleToDeploy;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

@Component("prepareModulesDeploymentStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareModulesDeploymentStep extends SyncFlowableStep {

    @Inject
    private ApplicationConfiguration configuration;

    @Inject
    protected ProcessTypeParser processTypeParser;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.PREPARING_MODULES_DEPLOYMENT);

        // Get the list of cloud applications from the context:
        List<ModuleToDeploy> modulesToDeploy = getModulesToDeploy(execution.getContext());

        // Initialize the iteration over the applications list:
        execution.getContext()
            .setVariable(Constants.VAR_MODULES_COUNT, modulesToDeploy.size());
        execution.getContext()
            .setVariable(Constants.VAR_MODULES_INDEX, 0);
        execution.getContext()
            .setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_MODULES_INDEX);

        execution.getContext()
            .setVariable(Constants.VAR_CONTROLLER_POLLING_INTERVAL, configuration.getControllerPollingInterval());
        execution.getContext()
            .setVariable(Constants.VAR_PLATFORM_TYPE, configuration.getPlatformType()
                .toString());

        execution.getContext()
            .setVariable(Constants.REBUILD_APP_ENV, true);
        execution.getContext()
            .setVariable(Constants.SHOULD_UPLOAD_APPLICATION_CONTENT, true);
        execution.getContext()
            .setVariable(Constants.EXECUTE_ONE_OFF_TASKS, true);

        StepsUtil.setModulesToDeploy(execution.getContext(), modulesToDeploy);

        ProcessType processType = processTypeParser.getProcessType(execution.getContext());

        StepsUtil.setSkipUpdateConfigurationEntries(execution.getContext(), ProcessType.BLUE_GREEN_DEPLOY.equals(processType));

        return StepPhase.DONE;
    }
    
    protected List<ModuleToDeploy> getModulesToDeploy(DelegateExecution context){
        return StepsUtil.getAllModulesToDeploy(context);
    }

}

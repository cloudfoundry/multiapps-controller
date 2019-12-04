package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.core.model.BlueGreenApplicationNameSuffix;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import javax.inject.Named;
import java.util.List;

@Named("removeNewApplicationsSuffixStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RemoveNewApplicationsSuffixStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        if (!StepsUtil.getKeepOriginalAppNamesAfterDeploy(execution.getContext())) {
            return StepPhase.DONE;
        }
        getStepLogger().info(Messages.RENAMING_NEW_APPLICATIONS);

        List<String> appsToProcess = StepsUtil.getAppsToDeploy(execution.getContext());
        CloudControllerClient client = execution.getControllerClient();

        for (String appName : appsToProcess) {
            getStepLogger().debug(Messages.RENAMING_APPLICATION, appName);
            client.rename(appName, BlueGreenApplicationNameSuffix.removeSuffix(appName));
        }
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_RENAMING_NEW_APPLICATIONS;
    }

}

package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Named("stopApplicationUndeploymentStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StopApplicationUndeploymentStep extends UndeployAppStep {

    @Override
    protected StepPhase undeployApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy) {
        getStepLogger().info(Messages.STOPPING_APP, cloudApplicationToUndeploy.getName());
        client.stopApplication(cloudApplicationToUndeploy.getName());
        getStepLogger().debug(Messages.APP_STOPPED, cloudApplicationToUndeploy.getName());

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return MessageFormat.format(Messages.ERROR_STOPPING_APP, StepsUtil.getApp(context)
                                                                          .getName());
    }

    @Override
    protected HookPhase getHookPhaseBeforeStep(DelegateExecution context) {
        return HookPhase.APPLICATION_BEFORE_STOP_LIVE;
    }

    @Override
    protected HookPhase getHookPhaseAfterStep(DelegateExecution context) {
        return HookPhase.APPLICATION_AFTER_STOP_LIVE;
    }

}

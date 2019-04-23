package com.sap.cloud.lm.sl.cf.process.steps;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("stopApplicationUndeploymentStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StopApplicationUndeploymentStep extends UndeployAppStep {

    @Override
    protected StepPhase undeployApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy) {
        getStepLogger().info(Messages.STOPPING_APP, cloudApplicationToUndeploy.getName());
        client.stopApplication(cloudApplicationToUndeploy.getName());
        getStepLogger().debug(Messages.APP_STOPPED, cloudApplicationToUndeploy.getName());

        return StepPhase.DONE;
    }

}

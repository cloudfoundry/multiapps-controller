package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.core.model.BlueGreenApplicationNameSuffix;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named("removeNewApplicationsSuffixStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RemoveNewApplicationsSuffixStep extends SyncFlowableStep {

    @Inject
    private ConfigurationSubscriptionService subscriptionService;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        if (!StepsUtil.getKeepOriginalAppNamesAfterDeploy(execution.getContext())) {
            return StepPhase.DONE;
        }

        List<String> appsToProcess = StepsUtil.getAppsToDeploy(execution.getContext());
        CloudControllerClient client = execution.getControllerClient();

        for (String appName : appsToProcess) {
            String newName = BlueGreenApplicationNameSuffix.removeSuffix(appName);
            getStepLogger().info(Messages.RENAMING_APPLICATION_0_TO_1, appName, newName);
            client.rename(appName, newName);
        }

        String mtaId = (String) execution.getContext()
                                         .getVariable(Constants.PARAM_MTA_ID);
        String spaceId = StepsUtil.getSpaceId(execution.getContext());
        updateConfigurationSubscribers(appsToProcess, mtaId, spaceId);

        return StepPhase.DONE;
    }

    private void updateConfigurationSubscribers(List<String> appsToProcess, String mtaId, String spaceId) {
        List<ConfigurationSubscription> subscriptions = subscriptionService.createQuery()
                                                                           .mtaId(mtaId)
                                                                           .spaceId(spaceId)
                                                                           .list();
        for (ConfigurationSubscription subscription : subscriptions) {
            if (appsToProcess.contains(subscription.getAppName())) {
                String newAppName = BlueGreenApplicationNameSuffix.removeSuffix(subscription.getAppName());
                getStepLogger().debug(Messages.UPDATING_CONFIGURATION_SUBSCRIPTION_0_WITH_NAME_1, subscription.getAppName(), newAppName);
                updateConfigurationSubscription(subscription, newAppName);
            }
        }
    }

    private void updateConfigurationSubscription(ConfigurationSubscription subscription, String newAppName) {
        subscriptionService.update(subscription.getId(), createNewSubscription(subscription, newAppName));
    }

    private ConfigurationSubscription createNewSubscription(ConfigurationSubscription subscription, String newAppName) {
        return new ConfigurationSubscription(subscription.getId(),
                                             subscription.getMtaId(),
                                             subscription.getSpaceId(),
                                             newAppName,
                                             subscription.getFilter(),
                                             subscription.getModuleDto(),
                                             subscription.getResourceDto());
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_RENAMING_NEW_APPLICATIONS;
    }

}

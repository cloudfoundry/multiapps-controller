package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.core.model.BlueGreenApplicationNameSuffix;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
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
        getStepLogger().info(Messages.RENAMING_NEW_APPLICATIONS);

        List<String> appsToProcess = StepsUtil.getAppsToDeploy(execution.getContext());
        CloudControllerClient client = execution.getControllerClient();

        for (String appName : appsToProcess) {
            getStepLogger().debug(Messages.RENAMING_APPLICATION, appName);
            client.rename(appName, BlueGreenApplicationNameSuffix.removeSuffix(appName));
        }

        String mtaId = (String) execution.getContext()
                                         .getVariable(Constants.PARAM_MTA_ID);
        updateConfigurationSubscribers(appsToProcess, mtaId);

        return StepPhase.DONE;
    }

    private void updateConfigurationSubscribers(List<String> appsToProcess, String mtaId) {
        List<ConfigurationSubscription> subscriptions = subscriptionService.createQuery()
                                                                           .mtaId(mtaId)
                                                                           .list();
        for (ConfigurationSubscription subscription : subscriptions) {
            if (appsToProcess.contains(subscription.getAppName())) {
                updateConfigurationSubscription(subscription);
            }
        }
    }

    private void updateConfigurationSubscription(ConfigurationSubscription subscription) {
        subscriptionService.update(subscription.getId(), createNewSubscription(subscription));
    }

    private ConfigurationSubscription createNewSubscription(ConfigurationSubscription subscription) {
        return new ConfigurationSubscription(subscription.getId(),
                                             subscription.getMtaId(),
                                             subscription.getSpaceId(),
                                             BlueGreenApplicationNameSuffix.removeSuffix(subscription.getAppName()),
                                             subscription.getFilter(),
                                             subscription.getModuleDto(),
                                             subscription.getResourceDto());
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_RENAMING_NEW_APPLICATIONS;
    }

}

package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.CreateOrUpdateServiceBrokersStep.getServiceBrokerNames;

import java.text.MessageFormat;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributesGetter;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("deleteServiceBrokersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteServiceBrokersStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        try {
            getStepLogger().info(Messages.DELETING_SERVICE_BROKERS);

            List<CloudApplication> appsToUndeploy = StepsUtil.getAppsToUndeploy(execution.getContext());
            CloudFoundryOperations client = execution.getCloudFoundryClient();
            List<String> serviceBrokersToCreate = getServiceBrokerNames(StepsUtil.getServiceBrokersToCreate(execution.getContext()));

            for (CloudApplication app : appsToUndeploy) {
                deleteServiceBrokerIfNecessary(execution.getContext(), app, serviceBrokersToCreate, client);
            }

            getStepLogger().debug(Messages.SERVICE_BROKERS_DELETED);
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DELETING_SERVICE_BROKERS);
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_DELETING_SERVICE_BROKERS);
            throw e;
        }
    }

    private void deleteServiceBrokerIfNecessary(DelegateExecution context, CloudApplication app, List<String> serviceBrokersToCreate,
        CloudFoundryOperations client) throws SLException {
        ApplicationAttributesGetter attributesGetter = ApplicationAttributesGetter.forApplication(app);
        if (!attributesGetter.getAttribute(SupportedParameters.CREATE_SERVICE_BROKER, Boolean.class, false)) {
            return;
        }
        String name = attributesGetter.getAttribute(SupportedParameters.SERVICE_BROKER_NAME, String.class, app.getName());

        CloudServiceBroker serviceBroker = client.getServiceBroker(name, false);
        if (serviceBroker != null && !serviceBrokersToCreate.contains(name)) {
            try {
                getStepLogger().info(MessageFormat.format(Messages.DELETING_SERVICE_BROKER, name, app.getName()));
                client.deleteServiceBroker(name);
                getStepLogger().debug(MessageFormat.format(Messages.DELETED_SERVICE_BROKER, name, app.getName()));
            } catch (CloudFoundryException e) {
                switch (e.getStatusCode()) {
                    case FORBIDDEN:
                        if (shouldSucceed(context)) {
                            getStepLogger().warn(Messages.DELETE_OF_SERVICE_BROKERS_FAILED_403, name);
                            return;
                        }
                    default:
                        throw e;
                }
            }
        }
    }

    private boolean shouldSucceed(DelegateExecution context) {
        return (Boolean) context.getVariable(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS);
    }

}

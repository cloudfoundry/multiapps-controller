package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributes;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ExceptionMessageTailMapper;
import com.sap.cloud.lm.sl.cf.process.util.ExceptionMessageTailMapper.CloudComponents;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("deleteServiceBrokersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteServiceBrokersStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.DELETING_SERVICE_BROKERS);

        List<CloudApplication> appsToUndeploy = context.getVariable(Variables.APPS_TO_UNDEPLOY);
        CloudControllerClient client = context.getControllerClient();
        List<String> createdOrUpdatedServiceBrokers = getCreatedOrUpdatedServiceBrokerNames(context);

        for (CloudApplication app : appsToUndeploy) {
            deleteServiceBrokerIfNecessary(context, app, createdOrUpdatedServiceBrokers, client);
        }

        getStepLogger().debug(Messages.SERVICE_BROKERS_DELETED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DELETING_SERVICE_BROKERS;
    }

    @Override
    protected String getStepErrorMessageAdditionalDescription(ProcessContext context) {
        String offering = context.getVariable(Variables.SERVICE_OFFERING);
        return ExceptionMessageTailMapper.map(configuration, CloudComponents.SERVICE_BROKERS, offering);
    }

    protected List<String> getCreatedOrUpdatedServiceBrokerNames(ProcessContext context) {
        return StepsUtil.getCreatedOrUpdatedServiceBrokerNames(context);
    }

    private void deleteServiceBrokerIfNecessary(ProcessContext context, CloudApplication app, List<String> createdOrUpdatedServiceBrokers,
                                                CloudControllerClient client) {
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app);
        if (!appAttributes.get(SupportedParameters.CREATE_SERVICE_BROKER, Boolean.class, false)) {
            return;
        }
        String name = appAttributes.get(SupportedParameters.SERVICE_BROKER_NAME, String.class, app.getName());

        CloudServiceBroker serviceBroker = client.getServiceBroker(name, false);
        if (serviceBroker != null && !createdOrUpdatedServiceBrokers.contains(name)) {
            try {
                getStepLogger().info(MessageFormat.format(Messages.DELETING_SERVICE_BROKER, name, app.getName()));
                client.deleteServiceBroker(name);
                getStepLogger().debug(MessageFormat.format(Messages.DELETED_SERVICE_BROKER, name, app.getName()));
            } catch (CloudOperationException e) {
                switch (e.getStatusCode()) {
                    case FORBIDDEN:
                        if (shouldSucceed(context)) {
                            getStepLogger().warn(Messages.DELETE_OF_SERVICE_BROKERS_FAILED_403, name);
                            return;
                        }
                        context.setVariable(Variables.SERVICE_OFFERING, name);
                        throw new CloudServiceBrokerException(e);
                    case BAD_GATEWAY:
                        context.setVariable(Variables.SERVICE_OFFERING, name);
                        throw new CloudServiceBrokerException(e);
                    default:
                        throw e;
                }
            }
        }
    }

    private boolean shouldSucceed(ProcessContext context) {
        return context.getVariable(Variables.NO_FAIL_ON_MISSING_PERMISSIONS);
    }

}

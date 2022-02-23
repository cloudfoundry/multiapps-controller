package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper.CloudComponents;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.CloudServiceBrokerException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;

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
                    case CONFLICT:
                        getStepLogger().warn(Messages.DELETE_OF_SERVICE_BROKERS_FAILED_409, name);
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

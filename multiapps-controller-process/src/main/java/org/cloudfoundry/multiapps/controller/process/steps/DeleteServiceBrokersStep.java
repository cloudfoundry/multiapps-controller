package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

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
public class DeleteServiceBrokersStep extends TimeoutAsyncFlowableStep {

    private static final Duration ASYNC_JOB_POLLING_TIMEOUT = Duration.ofMinutes(30);

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        getStepLogger().debug(Messages.DELETING_SERVICE_BROKERS);

        List<CloudApplication> appsToUndeploy = context.getVariable(Variables.APPS_TO_UNDEPLOY);
        CloudControllerClient client = context.getControllerClient();
        List<String> createdOrUpdatedServiceBrokers = getCreatedOrUpdatedServiceBrokerNames(context);

        Map<String, CloudServiceBroker> serviceBrokersToDelete = getServiceBrokersToDelete(appsToUndeploy, createdOrUpdatedServiceBrokers,
                                                                                           client);

        Map<String, String> serviceBrokerNamesJobIds = deleteServiceBrokers(context, serviceBrokersToDelete, client);

        if (serviceBrokerNamesJobIds.isEmpty()) {
            getStepLogger().debug(Messages.SERVICE_BROKERS_DELETED);
            return StepPhase.DONE;
        }

        context.setVariable(Variables.SERVICE_BROKER_NAMES_JOB_IDS, serviceBrokerNamesJobIds);
        return StepPhase.POLL;
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

    @Override
    public Duration getTimeout(ProcessContext context) {
        return ASYNC_JOB_POLLING_TIMEOUT;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return List.of(new PollServiceBrokersOperationsExecution());
    }

    protected List<String> getCreatedOrUpdatedServiceBrokerNames(ProcessContext context) {
        return StepsUtil.getCreatedOrUpdatedServiceBrokerNames(context);
    }

    private Map<String, CloudServiceBroker> getServiceBrokersToDelete(List<CloudApplication> appsToUndeploy,
                                                                      List<String> createdOrUpdatedServiceBrokers,
                                                                      CloudControllerClient client) {
        Map<String, CloudServiceBroker> serviceBrokersToDelete = new HashMap<>();
        for (CloudApplication app : appsToUndeploy) {
            Optional<CloudServiceBroker> optionalServiceBroker = getServiceBrokerToDelete(app, createdOrUpdatedServiceBrokers, client);
            if (optionalServiceBroker.isPresent()) {
                serviceBrokersToDelete.put(app.getName(), optionalServiceBroker.get());
            }
        }
        return serviceBrokersToDelete;
    }

    private Optional<CloudServiceBroker> getServiceBrokerToDelete(CloudApplication app, List<String> createdOrUpdatedServiceBrokers,
                                                                  CloudControllerClient client) {
        var appEnv = client.getApplicationEnvironment(app.getGuid());
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app, appEnv);
        if (!appAttributes.get(SupportedParameters.CREATE_SERVICE_BROKER, Boolean.class, false)) {
            return Optional.empty();
        }

        String serviceBrokerName = appAttributes.get(SupportedParameters.SERVICE_BROKER_NAME, String.class, app.getName());
        return Optional.ofNullable(client.getServiceBroker(serviceBrokerName, false))
                       .filter(serviceBroker -> !createdOrUpdatedServiceBrokers.contains(serviceBroker.getName()));
    }

    private Map<String, String> deleteServiceBrokers(ProcessContext context, Map<String, CloudServiceBroker> serviceBrokersToDelete,
                                                     CloudControllerClient client) {
        Map<String, String> serviceBrokerNamesJobIds = new HashMap<>();
        for (Entry<String, CloudServiceBroker> appNameServiceBroker : serviceBrokersToDelete.entrySet()) {
            String appName = appNameServiceBroker.getKey();
            CloudServiceBroker serviceBroker = appNameServiceBroker.getValue();
            try {
                getStepLogger().info(MessageFormat.format(Messages.DELETING_SERVICE_BROKER, serviceBroker.getName(), appName));
                String jobId = client.deleteServiceBroker(serviceBroker.getName());
                getStepLogger().debug(MessageFormat.format(Messages.DELETE_SERVICE_BROKER_TRIGERRED, serviceBroker.getName(), appName));
                if (jobId != null) {
                    serviceBrokerNamesJobIds.put(serviceBroker.getName(), jobId);
                }
            } catch (CloudOperationException e) {
                handleCloudOperationException(context, serviceBroker, e);
            }
        }
        return serviceBrokerNamesJobIds;
    }

    private void handleCloudOperationException(ProcessContext context, CloudServiceBroker serviceBroker, CloudOperationException e) {
        switch (e.getStatusCode()) {
            case FORBIDDEN:
                if (shouldSucceed(context)) {
                    getStepLogger().warn(Messages.DELETE_OF_SERVICE_BROKERS_FAILED_403, serviceBroker.getName());
                    return;
                }
                context.setVariable(Variables.SERVICE_OFFERING, serviceBroker.getName());
                throw new CloudServiceBrokerException(e);
            case BAD_GATEWAY:
                context.setVariable(Variables.SERVICE_OFFERING, serviceBroker.getName());
                throw new CloudServiceBrokerException(e);
            case CONFLICT:
                getStepLogger().warn(Messages.DELETE_OF_SERVICE_BROKERS_FAILED_409, serviceBroker.getName());
                throw new CloudServiceBrokerException(e);
            default:
                throw e;
        }
    }

    private boolean shouldSucceed(ProcessContext context) {
        return context.getVariable(Variables.NO_FAIL_ON_MISSING_PERMISSIONS);
    }

}

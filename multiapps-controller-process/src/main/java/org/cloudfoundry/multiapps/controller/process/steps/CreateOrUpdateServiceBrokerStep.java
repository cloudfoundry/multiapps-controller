package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
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
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBroker;

@Named("createOrUpdateServiceBrokerStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateOrUpdateServiceBrokerStep extends TimeoutAsyncFlowableStep {

    private static final Duration ASYNC_JOB_POLLING_TIMEOUT = Duration.ofMinutes(30);

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        getStepLogger().debug(Messages.CREATING_SERVICE_BROKERS);

        CloudServiceBroker serviceBroker = getServiceBrokerToCreate(context);
        if (serviceBroker == null) {
            return StepPhase.DONE;
        }
        getStepLogger().debug(MessageFormat.format(Messages.SERVICE_BROKER, SecureSerialization.toJson(serviceBroker)));

        CloudControllerClient client = context.getControllerClient();
        List<CloudServiceBroker> existingServiceBrokers = client.getServiceBrokers();
        List<String> existingServiceBrokerNames = getServiceBrokerNames(existingServiceBrokers);

        String jobId = null;
        if (existingServiceBrokerNames.contains(serviceBroker.getName())) {
            CloudServiceBroker existingBroker = findServiceBroker(existingServiceBrokers, serviceBroker.getName());
            serviceBroker = mergeServiceBrokerMetadata(serviceBroker, existingBroker);
            jobId = updateServiceBroker(context, serviceBroker, client);
            getStepLogger().debug(MessageFormat.format(Messages.UPDATE_SERVICE_BROKER_TRIGERRED, serviceBroker.getName()));
        } else {
            jobId = createServiceBroker(context, serviceBroker, client);
            getStepLogger().debug(MessageFormat.format(Messages.CREATE_SERVICE_BROKER_TRIGERRED, serviceBroker.getName()));
        }

        if (jobId != null) {
            context.setVariable(Variables.SERVICE_BROKER_ASYNC_JOB_ID, jobId);
            context.setVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER, serviceBroker);
            return StepPhase.POLL;
        }

        context.setVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER, serviceBroker);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_CREATING_SERVICE_BROKERS;
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
        return List.of(new PollServiceBrokerOperationsExecution());
    }

    private CloudServiceBroker getServiceBrokerToCreate(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudServiceBroker serviceBroker = getServiceBrokerFromApp(context, app);
        if (serviceBroker == null) {
            return null;
        }
        String msg = MessageFormat.format(Messages.CONSTRUCTED_SERVICE_BROKER_FROM_APPLICATION, serviceBroker.getName(), app.getName());
        getStepLogger().debug(msg);
        return serviceBroker;
    }

    protected CloudServiceBroker getServiceBrokerFromApp(ProcessContext context, CloudApplication app) {
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app);
        if (!appAttributes.get(SupportedParameters.CREATE_SERVICE_BROKER, Boolean.class, false)) {
            return null;
        }

        String serviceBrokerName = appAttributes.get(SupportedParameters.SERVICE_BROKER_NAME, String.class, app.getName());
        String serviceBrokerUsername = appAttributes.get(SupportedParameters.SERVICE_BROKER_USERNAME, String.class);
        String serviceBrokerPassword = appAttributes.get(SupportedParameters.SERVICE_BROKER_PASSWORD, String.class);
        String serviceBrokerUrl = appAttributes.get(SupportedParameters.SERVICE_BROKER_URL, String.class);
        String serviceBrokerSpaceGuid = getServiceBrokerSpaceGuid(context, appAttributes);

        if (serviceBrokerName == null) {
            throw new ContentException(Messages.MISSING_SERVICE_BROKER_NAME, app.getName());
        }
        if (serviceBrokerUsername == null) {
            throw new ContentException(Messages.MISSING_SERVICE_BROKER_USERNAME, app.getName());
        }
        if (serviceBrokerPassword == null) {
            throw new ContentException(Messages.MISSING_SERVICE_BROKER_PASSWORD, app.getName());
        }
        if (serviceBrokerUrl == null) {
            throw new ContentException(Messages.MISSING_SERVICE_BROKER_URL, app.getName());
        }

        return ImmutableCloudServiceBroker.builder()
                                          .name(serviceBrokerName)
                                          .username(serviceBrokerUsername)
                                          .password(serviceBrokerPassword)
                                          .url(serviceBrokerUrl)
                                          .spaceGuid(serviceBrokerSpaceGuid)
                                          .build();
    }

    private String getServiceBrokerSpaceGuid(ProcessContext context, ApplicationAttributes appAttributes) {
        boolean isSpaceScoped = appAttributes.get(SupportedParameters.SERVICE_BROKER_SPACE_SCOPED, Boolean.class, false);
        return isSpaceScoped ? context.getVariable(Variables.SPACE_GUID) : null;
    }

    private CloudServiceBroker findServiceBroker(List<CloudServiceBroker> serviceBrokers, String name) {
        return serviceBrokers.stream()
                             .filter(broker -> broker.getName()
                                                     .equals(name))
                             .findFirst()
                             .orElseThrow(() -> new NotFoundException(MessageFormat.format(Messages.SERVICE_BROKER_0_DOES_NOT_EXIST,
                                                                                           name)));
    }

    private CloudServiceBroker mergeServiceBrokerMetadata(CloudServiceBroker serviceBroker, CloudServiceBroker existingBroker) {
        CloudServiceBroker mergedServiceBrokerMetadata = ImmutableCloudServiceBroker.copyOf(serviceBroker)
                                                                                    .withMetadata(existingBroker.getMetadata());
        if (existingBroker.getSpaceGuid() != null && mergedServiceBrokerMetadata.getSpaceGuid() == null) {
            getStepLogger().warn(MessageFormat.format(Messages.CANNOT_CHANGE_VISIBILITY_OF_SERVICE_BROKER_FROM_SPACE_SCOPED_TO_GLOBAL,
                                                      mergedServiceBrokerMetadata.getName()));
        } else if (existingBroker.getSpaceGuid() == null && serviceBroker.getSpaceGuid() != null) {
            getStepLogger().warn(MessageFormat.format(Messages.CANNOT_CHANGE_VISIBILITY_OF_SERVICE_BROKER_FROM_GLOBAL_TO_SPACE_SCOPED,
                                                      mergedServiceBrokerMetadata.getName()));
        }
        return mergedServiceBrokerMetadata;
    }

    public static List<String> getServiceBrokerNames(List<? extends CloudServiceBroker> serviceBrokers) {
        return serviceBrokers.stream()
                             .map(CloudServiceBroker::getName)
                             .collect(Collectors.toList());
    }

    protected String updateServiceBroker(ProcessContext context, CloudServiceBroker serviceBroker, CloudControllerClient client) {
        try {
            getStepLogger().info(MessageFormat.format(Messages.UPDATING_SERVICE_BROKER, serviceBroker.getName()));
            return client.updateServiceBroker(serviceBroker);
        } catch (CloudOperationException e) {
            switch (e.getStatusCode()) {
                case NOT_IMPLEMENTED:
                    getStepLogger().warn(Messages.UPDATE_OF_SERVICE_BROKERS_FAILED_501, serviceBroker.getName());
                    return null;
                case FORBIDDEN:
                    if (shouldSucceed(context)) {
                        getStepLogger().warn(Messages.UPDATE_OF_SERVICE_BROKERS_FAILED_403, serviceBroker.getName());
                        return null;
                    }
                    context.setVariable(Variables.SERVICE_OFFERING, serviceBroker.getName());
                    throw new CloudServiceBrokerException(e);
                case BAD_GATEWAY:
                    context.setVariable(Variables.SERVICE_OFFERING, serviceBroker.getName());
                    throw new CloudServiceBrokerException(e);
                default:
                    throw e;
            }
        }
    }

    private String createServiceBroker(ProcessContext context, CloudServiceBroker serviceBroker, CloudControllerClient client) {
        try {
            getStepLogger().info(MessageFormat.format(Messages.CREATING_SERVICE_BROKER, serviceBroker.getName()));
            return client.createServiceBroker(serviceBroker);
        } catch (CloudOperationException e) {
            switch (e.getStatusCode()) {
                case FORBIDDEN:
                    if (shouldSucceed(context)) {
                        getStepLogger().warn(Messages.CREATE_OF_SERVICE_BROKERS_FAILED_403, serviceBroker.getName());
                        return null;
                    }
                    throw new CloudServiceBrokerException(e);
                case BAD_GATEWAY:
                    throw new CloudServiceBrokerException(e);
                default:
                    throw e;
            }
        }
    }

    private boolean shouldSucceed(ProcessContext context) {
        return context.getVariable(Variables.NO_FAIL_ON_MISSING_PERMISSIONS);
    }

}

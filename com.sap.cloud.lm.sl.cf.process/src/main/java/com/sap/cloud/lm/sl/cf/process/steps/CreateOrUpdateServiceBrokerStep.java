package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBroker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributes;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ExceptionMessageTailMapper;
import com.sap.cloud.lm.sl.cf.process.util.ExceptionMessageTailMapper.CloudComponents;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Named("createOrUpdateServiceBrokerStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateOrUpdateServiceBrokerStep extends SyncFlowableStep {

    private final SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.CREATING_SERVICE_BROKERS);

        CloudServiceBroker serviceBroker = getServiceBrokerToCreate(context);
        if (serviceBroker == null) {
            return StepPhase.DONE;
        }
        getStepLogger().debug(MessageFormat.format(Messages.SERVICE_BROKER, secureSerializer.toJson(serviceBroker)));

        CloudControllerClient client = context.getControllerClient();
        List<CloudServiceBroker> existingServiceBrokers = client.getServiceBrokers();
        List<String> existingServiceBrokerNames = getServiceBrokerNames(existingServiceBrokers);

        if (existingServiceBrokerNames.contains(serviceBroker.getName())) {
            CloudServiceBroker existingBroker = findServiceBroker(existingServiceBrokers, serviceBroker.getName());
            serviceBroker = updateServiceBroker(context, serviceBroker, existingBroker, client);
        } else {
            createServiceBroker(context, serviceBroker, client);
        }

        context.setVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER, serviceBroker);
        getStepLogger().debug(Messages.SERVICE_BROKERS_CREATED);
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

    private CloudServiceBroker updateServiceBroker(ProcessContext context, CloudServiceBroker serviceBroker,
                                                   CloudServiceBroker existingBroker, CloudControllerClient client) {
        serviceBroker = ImmutableCloudServiceBroker.copyOf(serviceBroker)
                                                   .withMetadata(existingBroker.getMetadata());
        if (existingBroker.getSpaceGuid() != null && serviceBroker.getSpaceGuid() == null) {
            getStepLogger().warn(MessageFormat.format(Messages.CANNOT_CHANGE_VISIBILITY_OF_SERVICE_BROKER_FROM_SPACE_SCOPED_TO_GLOBAL,
                                                      serviceBroker.getName()));
        } else if (existingBroker.getSpaceGuid() == null && serviceBroker.getSpaceGuid() != null) {
            getStepLogger().warn(MessageFormat.format(Messages.CANNOT_CHANGE_VISIBILITY_OF_SERVICE_BROKER_FROM_GLOBAL_TO_SPACE_SCOPED,
                                                      serviceBroker.getName()));
        }
        updateServiceBroker(context, serviceBroker, client);
        return serviceBroker;
    }

    private CloudServiceBroker findServiceBroker(List<CloudServiceBroker> serviceBrokers, String name) {
        return serviceBrokers.stream()
                             .filter(broker -> broker.getName()
                                                     .equals(name))
                             .findFirst()
                             .orElseThrow(() -> new NotFoundException(MessageFormat.format(Messages.SERVICE_BROKER_0_DOES_NOT_EXIST,
                                                                                           name)));
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

    public static List<String> getServiceBrokerNames(List<? extends CloudServiceBroker> serviceBrokers) {
        return serviceBrokers.stream()
                             .map(CloudServiceBroker::getName)
                             .collect(Collectors.toList());
    }

    protected void updateServiceBroker(ProcessContext context, CloudServiceBroker serviceBroker, CloudControllerClient client) {
        try {
            getStepLogger().info(MessageFormat.format(Messages.UPDATING_SERVICE_BROKER, serviceBroker.getName()));
            client.updateServiceBroker(serviceBroker);
            getStepLogger().debug(MessageFormat.format(Messages.UPDATED_SERVICE_BROKER, serviceBroker.getName()));
        } catch (CloudOperationException e) {
            switch (e.getStatusCode()) {
                case NOT_IMPLEMENTED:
                    getStepLogger().warn(Messages.UPDATE_OF_SERVICE_BROKERS_FAILED_501, serviceBroker.getName());
                    break;
                case FORBIDDEN:
                    if (shouldSucceed(context)) {
                        getStepLogger().warn(Messages.UPDATE_OF_SERVICE_BROKERS_FAILED_403, serviceBroker.getName());
                        return;
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

    private void createServiceBroker(ProcessContext context, CloudServiceBroker serviceBroker, CloudControllerClient client) {
        try {
            getStepLogger().info(MessageFormat.format(Messages.CREATING_SERVICE_BROKER, serviceBroker.getName()));
            client.createServiceBroker(serviceBroker);
            getStepLogger().debug(MessageFormat.format(Messages.CREATED_SERVICE_BROKER, serviceBroker.getName()));
        } catch (CloudOperationException e) {
            switch (e.getStatusCode()) {
                case FORBIDDEN:
                    if (shouldSucceed(context)) {
                        getStepLogger().warn(Messages.CREATE_OF_SERVICE_BROKERS_FAILED_403, serviceBroker.getName());
                        return;
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

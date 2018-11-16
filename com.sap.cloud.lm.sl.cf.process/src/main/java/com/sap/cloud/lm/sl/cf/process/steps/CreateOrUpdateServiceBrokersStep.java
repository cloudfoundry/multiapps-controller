package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceBrokerExtended;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceBrokerCreator;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceBrokersGetter;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributes;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;

@Component("createOrUpdateServiceBrokersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateOrUpdateServiceBrokersStep extends SyncFlowableStep {

    @Inject
    private ServiceBrokerCreator serviceBrokerCreator;
    @Inject
    private ServiceBrokersGetter serviceBrokersGetter;
    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();
    @Inject
    private ApplicationConfiguration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.CREATING_SERVICE_BROKERS);

            CloudControllerClient client = execution.getControllerClient();
            List<CloudServiceBrokerExtended> existingServiceBrokers = serviceBrokersGetter.getServiceBrokers(client);
            List<CloudServiceBrokerExtended> serviceBrokersToCreate = getServiceBrokersToCreate(
                mapApplicationsFromContextToCloudApplications(execution, client), execution.getContext());
            getStepLogger().debug(MessageFormat.format(Messages.SERVICE_BROKERS, secureSerializer.toJson(serviceBrokersToCreate)));
            List<String> existingServiceBrokerNames = getServiceBrokerNames(existingServiceBrokers);

            for (CloudServiceBrokerExtended serviceBroker : serviceBrokersToCreate) {
                if (existingServiceBrokerNames.contains(serviceBroker.getName())) {
                    CloudServiceBrokerExtended existingBroker = findServiceBroker(existingServiceBrokers, serviceBroker.getName());
                    updateServiceBroker(execution.getContext(), serviceBroker, existingBroker, client);
                } else {
                    createServiceBroker(execution.getContext(), serviceBroker, client);
                }
            }

            StepsUtil.setServiceBrokersToCreate(execution.getContext(), serviceBrokersToCreate);
            getStepLogger().debug(Messages.SERVICE_BROKERS_CREATED);
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_CREATING_SERVICE_BROKERS);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_CREATING_SERVICE_BROKERS);
            throw e;
        }
    }

    private List<CloudApplication> mapApplicationsFromContextToCloudApplications(ExecutionWrapper execution,
        CloudControllerClient client) {
        return StepsUtil.getAppsToDeploy(execution.getContext())
            .stream()
            .map(app -> client.getApplication(app.getName()))
            .collect(Collectors.toList());
    }

    private void updateServiceBroker(DelegateExecution context, CloudServiceBrokerExtended serviceBroker,
        CloudServiceBrokerExtended existingBroker, CloudControllerClient client) {
        serviceBroker.setMeta(existingBroker.getMeta());
        if (existingBroker.getSpaceGuid() != null && serviceBroker.getSpaceGuid() == null) {
            getStepLogger().warn(MessageFormat.format(Messages.CANNOT_CHANGE_VISIBILITY_OF_SERVICE_BROKER_FROM_SPACE_SCOPED_TO_GLOBAL,
                serviceBroker.getName()));
        } else if (existingBroker.getSpaceGuid() == null && serviceBroker.getSpaceGuid() != null) {
            getStepLogger().warn(MessageFormat.format(Messages.CANNOT_CHANGE_VISIBILITY_OF_SERVICE_BROKER_FROM_GLOBAL_TO_SPACE_SCOPED,
                serviceBroker.getName()));
        }
        updateServiceBroker(context, serviceBroker, client);
    }

    private CloudServiceBrokerExtended findServiceBroker(List<CloudServiceBrokerExtended> serviceBrokers, String name) {
        return serviceBrokers.stream()
            .filter(broker -> broker.getName()
                .equals(name))
            .findFirst()
            .get();
    }

    private List<CloudServiceBrokerExtended> getServiceBrokersToCreate(List<CloudApplication> appsToDeploy, DelegateExecution context) {
        List<CloudServiceBrokerExtended> serviceBrokersToCreate = new ArrayList<>();
        for (CloudApplication app : appsToDeploy) {
            CloudServiceBrokerExtended serviceBroker = getServiceBrokerFromApp(app, context);
            if (serviceBroker == null) {
                continue;
            }
            String msg = MessageFormat.format(Messages.CONSTRUCTED_SERVICE_BROKER_FROM_APPLICATION, serviceBroker.getName(), app.getName());
            getStepLogger().debug(msg);
            serviceBrokersToCreate.add(serviceBroker);
        }
        return serviceBrokersToCreate;
    }

    protected CloudServiceBrokerExtended getServiceBrokerFromApp(CloudApplication app, DelegateExecution context) {
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app);
        if (!appAttributes.get(SupportedParameters.CREATE_SERVICE_BROKER, Boolean.class, false)) {
            return null;
        }

        String serviceBrokerName = appAttributes.get(SupportedParameters.SERVICE_BROKER_NAME, String.class, app.getName());
        String serviceBrokerUsername = appAttributes.get(SupportedParameters.SERVICE_BROKER_USERNAME, String.class);
        String serviceBrokerPassword = appAttributes.get(SupportedParameters.SERVICE_BROKER_PASSWORD, String.class);
        String serviceBrokerUrl = appAttributes.get(SupportedParameters.SERVICE_BROKER_URL, String.class);
        String serviceBrokerSpaceGuid = getServiceBrokerSpaceGuid(context, serviceBrokerName, appAttributes);

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

        return new CloudServiceBrokerExtended(serviceBrokerName, serviceBrokerUrl, serviceBrokerUsername, serviceBrokerPassword,
            serviceBrokerSpaceGuid);
    }

    private String getServiceBrokerSpaceGuid(DelegateExecution context, String serviceBrokerName, ApplicationAttributes appAttributes) {
        PlatformType platformType = configuration.getPlatformType();
        boolean isSpaceScoped = appAttributes.get(SupportedParameters.SERVICE_BROKER_SPACE_SCOPED, Boolean.class, false);
        if (platformType == PlatformType.XS2 && isSpaceScoped) {
            getStepLogger()
                .warn(MessageFormat.format(Messages.CANNOT_CREATE_SPACE_SCOPED_SERVICE_BROKER_ON_THIS_PLATFORM, serviceBrokerName));
            return null;
        }
        return isSpaceScoped ? StepsUtil.getSpaceId(context) : null;
    }

    public static List<String> getServiceBrokerNames(List<? extends CloudServiceBroker> serviceBrokers) {
        return serviceBrokers.stream()
            .map(CloudServiceBroker::getName)
            .collect(Collectors.toList());
    }

    protected void updateServiceBroker(DelegateExecution context, CloudServiceBroker serviceBroker, CloudControllerClient client) {
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
                    throw new CloudServiceBrokerException(e);
                case BAD_GATEWAY:
                    throw new CloudServiceBrokerException(e);
                default:
                    throw e;
            }
        }
    }

    private void createServiceBroker(DelegateExecution context, CloudServiceBrokerExtended serviceBroker, CloudControllerClient client) {
        try {
            getStepLogger().info(MessageFormat.format(Messages.CREATING_SERVICE_BROKER, serviceBroker.getName()));
            serviceBrokerCreator.createServiceBroker(client, serviceBroker);
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

    private boolean shouldSucceed(DelegateExecution context) {
        return (Boolean) context.getVariable(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS);
    }

}

package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceBrokerExtended;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceBrokerCreator;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceBrokersGetter;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributesGetter;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("createServiceBrokersStep")
public class CreateServiceBrokersStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateServiceBrokersStep.class);

    @Inject
    private ServiceBrokerCreator serviceBrokerCreator;
    @Inject
    private ServiceBrokersGetter serviceBrokersGetter;
    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();
    protected Supplier<PlatformType> platformTypeSupplier = () -> ConfigurationUtil.getPlatformType();

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("createServiceBrokersTask").displayName("Create Service Brokers").description(
            "Create Service Brokers").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.CREATING_SERVICE_BROKERS, LOGGER);

            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);
            List<CloudServiceBrokerExtended> existingServiceBrokers = serviceBrokersGetter.getServiceBrokers(client);
            List<CloudServiceBrokerExtended> serviceBrokersToCreate = getServiceBrokersToCreate(StepsUtil.getAppsToDeploy(context),
                context);
            debug(context, MessageFormat.format(Messages.SERVICE_BROKERS, secureSerializer.toJson(serviceBrokersToCreate)), LOGGER);
            List<String> existingServiceBrokerNames = getServiceBrokerNames(existingServiceBrokers);

            for (CloudServiceBrokerExtended serviceBroker : serviceBrokersToCreate) {
                if (existingServiceBrokerNames.contains(serviceBroker.getName())) {
                    CloudServiceBrokerExtended existingBroker = findServiceBroker(existingServiceBrokers, serviceBroker.getName());
                    updateServiceBroker(context, serviceBroker, existingBroker, client);
                } else {
                    createServiceBroker(context, serviceBroker, client);
                }
            }

            StepsUtil.setServiceBrokersToCreate(context, serviceBrokersToCreate);
            debug(context, Messages.SERVICE_BROKERS_CREATED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_CREATING_SERVICE_BROKERS, e, LOGGER);
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, Messages.ERROR_CREATING_SERVICE_BROKERS, e, LOGGER);
            throw e;
        }
    }

    private void updateServiceBroker(DelegateExecution context, CloudServiceBrokerExtended serviceBroker,
        CloudServiceBrokerExtended existingBroker, CloudFoundryOperations client) {
        serviceBroker.setMeta(existingBroker.getMeta());
        if (existingBroker.getSpaceGuid() != null && serviceBroker.getSpaceGuid() == null) {
            warn(context, MessageFormat.format(Messages.CANNOT_CHANGE_VISIBILITY_OF_SERVICE_BROKER_FROM_SPACE_SCOPED_TO_GLOBAL,
                serviceBroker.getName()), LOGGER);
        } else if (existingBroker.getSpaceGuid() == null && serviceBroker.getSpaceGuid() != null) {
            warn(context, MessageFormat.format(Messages.CANNOT_CHANGE_VISIBILITY_OF_SERVICE_BROKER_FROM_GLOBAL_TO_SPACE_SCOPED,
                serviceBroker.getName()), LOGGER);
        }
        updateServiceBroker(context, serviceBroker, client);
    }

    private CloudServiceBrokerExtended findServiceBroker(List<CloudServiceBrokerExtended> serviceBrokers, String name) {
        return serviceBrokers.stream().filter((broker) -> broker.getName().equals(name)).findFirst().get();
    }

    private List<CloudServiceBrokerExtended> getServiceBrokersToCreate(List<CloudApplicationExtended> appsToDeploy,
        DelegateExecution context) throws SLException {
        List<CloudServiceBrokerExtended> serviceBrokersToCreate = new ArrayList<>();
        for (CloudApplicationExtended app : appsToDeploy) {
            CloudServiceBrokerExtended serviceBroker = getServiceBrokerFromApp(app, context);
            if (serviceBroker == null) {
                continue;
            }
            String msg = MessageFormat.format(Messages.CONSTRUCTED_SERVICE_BROKER_FROM_APPLICATION, serviceBroker.getName(), app.getName());
            debug(context, msg, LOGGER);
            serviceBrokersToCreate.add(serviceBroker);
        }
        return serviceBrokersToCreate;
    }

    private CloudServiceBrokerExtended getServiceBrokerFromApp(CloudApplicationExtended app, DelegateExecution context) throws SLException {
        ApplicationAttributesGetter attributesGetter = ApplicationAttributesGetter.forApplication(app);
        if (!attributesGetter.getAttribute(SupportedParameters.CREATE_SERVICE_BROKER, Boolean.class, false)) {
            return null;
        }

        String serviceBrokerName = attributesGetter.getAttribute(SupportedParameters.SERVICE_BROKER_NAME, String.class, app.getName());
        String serviceBrokerUrl = attributesGetter.getAttribute(SupportedParameters.SERVICE_BROKER_URL, String.class);
        String serviceBrokerPassword = attributesGetter.getAttribute(SupportedParameters.SERVICE_BROKER_PASSWORD, String.class);
        String serviceBrokerUser = attributesGetter.getAttribute(SupportedParameters.SERVICE_BROKER_USER, String.class);
        String serviceBrokerSpaceGuid = getServiceBrokerSpaceGuid(context, serviceBrokerName, attributesGetter);

        if (serviceBrokerUser == null) {
            throw new SLException(Messages.MISSING_SERVICE_BROKER_USER, app.getName());
        }
        if (serviceBrokerUrl == null) {
            throw new SLException(Messages.MISSING_SERVICE_BROKER_URL, app.getName());
        }
        if (serviceBrokerName == null) {
            throw new SLException(Messages.MISSING_SERVICE_BROKER_NAME, app.getName());
        }

        return getCloudServiceBroker(serviceBrokerName, serviceBrokerUser, serviceBrokerPassword, serviceBrokerUrl, serviceBrokerSpaceGuid);
    }

    private String getServiceBrokerSpaceGuid(DelegateExecution context, String serviceBrokerName,
        ApplicationAttributesGetter attributesGetter) {
        PlatformType platformType = platformTypeSupplier.get();
        boolean isSpaceScoped = attributesGetter.getAttribute(SupportedParameters.SERVICE_BROKER_SPACE_SCOPED, Boolean.class, false);
        if (platformType == PlatformType.XS2 && isSpaceScoped) {
            warn(context, MessageFormat.format(Messages.CANNOT_CREATE_SPACE_SCOPED_SERVICE_BROKER_ON_THIS_PLATFORM, serviceBrokerName),
                LOGGER);
            return null;
        }
        return isSpaceScoped ? StepsUtil.getSpaceId(context) : null;
    }

    public static List<String> getServiceBrokerNames(List<? extends CloudServiceBroker> serviceBrokers) {
        return serviceBrokers.stream().map((broker) -> broker.getName()).collect(Collectors.toList());
    }

    protected void updateServiceBroker(DelegateExecution context, CloudServiceBroker serviceBroker, CloudFoundryOperations client) {
        try {
            info(context, MessageFormat.format(Messages.UPDATING_SERVICE_BROKER, serviceBroker.getName()), LOGGER);
            client.updateServiceBroker(serviceBroker);
            debug(context, MessageFormat.format(Messages.UPDATED_SERVICE_BROKER, serviceBroker.getName()), LOGGER);
        } catch (CloudFoundryException e) {
            switch (e.getStatusCode()) {
                case NOT_IMPLEMENTED:
                    warn(context, format(Messages.UPDATE_OF_SERVICE_BROKERS_FAILED_501, serviceBroker.getName()), LOGGER);
                    break;
                case FORBIDDEN:
                    if (shouldSucceed(context)) {
                        warn(context, format(Messages.UPDATE_OF_SERVICE_BROKERS_FAILED_403, serviceBroker.getName()), LOGGER);
                        return;
                    }
                default:
                    throw e;
            }
        }
    }

    private void createServiceBroker(DelegateExecution context, CloudServiceBrokerExtended serviceBroker, CloudFoundryOperations client) {
        try {
            info(context, MessageFormat.format(Messages.CREATING_SERVICE_BROKER, serviceBroker.getName()), LOGGER);
            serviceBrokerCreator.createServiceBroker(client, serviceBroker);
            debug(context, MessageFormat.format(Messages.CREATED_SERVICE_BROKER, serviceBroker.getName()), LOGGER);
        } catch (CloudFoundryException e) {
            switch (e.getStatusCode()) {
                case FORBIDDEN:
                    if (shouldSucceed(context)) {
                        warn(context, format(Messages.CREATE_OF_SERVICE_BROKERS_FAILED_403, serviceBroker.getName()), LOGGER);
                        return;
                    }
                default:
                    throw e;
            }
        }
    }

    private CloudServiceBrokerExtended getCloudServiceBroker(String name, String user, String password, String url, String spaceGuid) {
        return new CloudServiceBrokerExtended(name, url, user, password, spaceGuid);
    }

    private boolean shouldSucceed(DelegateExecution context) {
        return (Boolean) context.getVariable(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS);
    }

}

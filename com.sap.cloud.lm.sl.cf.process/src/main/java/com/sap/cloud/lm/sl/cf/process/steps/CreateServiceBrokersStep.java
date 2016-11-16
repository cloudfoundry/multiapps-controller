package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("createServiceBrokersStep")
public class CreateServiceBrokersStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateServiceBrokersStep.class);

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    public static StepMetadata getMetadata() {
        return new StepMetadata("createServiceBrokersTask", "Create Service Brokers", "Create Service Brokers");
    }

    protected Function<DelegateExecution, CloudFoundryOperations> clientSupplier = (context) -> getCloudFoundryClient(context, LOGGER);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.CREATING_SERVICE_BROKERS, LOGGER);

            CloudFoundryOperations client = clientSupplier.apply(context);
            List<CloudServiceBroker> existingServiceBrokers = client.getServiceBrokers();
            List<CloudServiceBroker> serviceBrokersToCreate = getServiceBrokersToCreate(StepsUtil.getAppsToDeploy(context), context);
            debug(context, MessageFormat.format(Messages.SERVICE_BROKERS, secureSerializer.toJson(serviceBrokersToCreate)), LOGGER);
            List<String> existingServiceBrokerNames = getServiceBrokerNames(existingServiceBrokers);

            for (CloudServiceBroker serviceBroker : serviceBrokersToCreate) {
                if (existingServiceBrokerNames.contains(serviceBroker.getName())) {
                    CloudServiceBroker existingBroker = existingServiceBrokers.stream().filter(
                        (broker) -> broker.getName().equals(serviceBroker.getName())).findFirst().get();
                    serviceBroker.setMeta(existingBroker.getMeta());
                    updateServiceBroker(context, serviceBroker, client);
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

    private List<CloudServiceBroker> getServiceBrokersToCreate(List<CloudApplicationExtended> appsToDeploy, DelegateExecution context)
        throws SLException {
        List<CloudServiceBroker> serviceBrokersToCreate = new ArrayList<>();
        for (CloudApplicationExtended app : appsToDeploy) {
            CloudServiceBroker serviceBroker = getServiceBrokerFromApp(app, context);
            if (serviceBroker == null) {
                continue;
            }
            String msg = MessageFormat.format(Messages.CONSTRUCTED_SERVICE_BROKER_FROM_APPLICATION, serviceBroker.getName(), app.getName());
            debug(context, msg, LOGGER);
            serviceBrokersToCreate.add(serviceBroker);
        }
        return serviceBrokersToCreate;
    }

    private CloudServiceBroker getServiceBrokerFromApp(CloudApplicationExtended app, DelegateExecution context) throws SLException {
        if (!StepsUtil.getAppAttribute(app, SupportedParameters.CREATE_SERVICE_BROKER, false)) {
            return null;
        }

        String serviceBrokerName = StepsUtil.getAppAttribute(app, SupportedParameters.SERVICE_BROKER_NAME, app.getName());
        String serviceBrokerUrl = StepsUtil.getAppAttribute(app, SupportedParameters.SERVICE_BROKER_URL, null);
        String serviceBrokerPasswordd = StepsUtil.getAppAttribute(app, SupportedParameters.SERVICE_BROKER_PASSWORD, null);
        String serviceBrokerUser = StepsUtil.getAppAttribute(app, SupportedParameters.SERVICE_BROKER_USER, null);

        if (serviceBrokerUser == null) {
            throw new SLException(Messages.MISSING_SERVICE_BROKER_USER, app.getName());
        }
        if (serviceBrokerUrl == null) {
            throw new SLException(Messages.MISSING_SERVICE_BROKER_URL, app.getName());
        }
        if (serviceBrokerName == null) {
            throw new SLException(Messages.MISSING_SERVICE_BROKER_NAME, app.getName());
        }

        return getCloudServiceBroker(serviceBrokerName, serviceBrokerUser, serviceBrokerPasswordd, serviceBrokerUrl);
    }

    public static List<String> getServiceBrokerNames(List<CloudServiceBroker> serviceBrokers) {
        return serviceBrokers.stream().map((broker) -> broker.getName()).collect(Collectors.toList());
    }

    private void updateServiceBroker(DelegateExecution context, CloudServiceBroker serviceBroker, CloudFoundryOperations client) {
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
                    warn(context, format(Messages.UPDATE_OF_SERVICE_BROKERS_FAILED_403, serviceBroker.getName()), LOGGER);
                    break;
                default:
                    throw e;
            }
        }
    }

    private void createServiceBroker(DelegateExecution context, CloudServiceBroker serviceBroker, CloudFoundryOperations client) {
        try {
            info(context, MessageFormat.format(Messages.CREATING_SERVICE_BROKER, serviceBroker.getName()), LOGGER);
            client.createServiceBroker(serviceBroker);
            debug(context, MessageFormat.format(Messages.CREATED_SERVICE_BROKER, serviceBroker.getName()), LOGGER);
        } catch (CloudFoundryException e) {
            switch (e.getStatusCode()) {
                case FORBIDDEN:
                    warn(context, format(Messages.CREATE_OF_SERVICE_BROKERS_FAILED_403, serviceBroker.getName()), LOGGER);
                    break;
                default:
                    throw e;
            }
        }
    }

    private CloudServiceBroker getCloudServiceBroker(String name, String user, String password, String url) {
        CloudServiceBroker serviceBroker = new CloudServiceBroker(url, user, password);
        if (name != null) {
            serviceBroker.setName(name);
        }
        return serviceBroker;
    }

}

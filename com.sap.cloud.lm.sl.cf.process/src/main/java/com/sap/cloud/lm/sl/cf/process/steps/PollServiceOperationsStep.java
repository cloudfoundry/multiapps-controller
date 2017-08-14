package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceInstanceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationExecutor;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("pollServiceOperationsStep")
public class PollServiceOperationsStep extends AbstractXS2ProcessStepWithBridge {

    private static final String LAST_SERVICE_OPERATION = "last_operation";
    private static final String SERVICE_NAME = "name";
    private static final String SERVICE_OPERATION_TYPE = "type";
    private static final String SERVICE_OPERATION_STATE = "state";
    private static final String SERVICE_OPERATION_DESCRIPTION = "description";

    private static final Logger LOGGER = LoggerFactory.getLogger(PollServiceOperationsStep.class);

    @Inject
    private ServiceInstanceGetter serviceInstanceGetter;

    private ServiceOperationExecutor serviceOperationExecutor = new ServiceOperationExecutor();

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("pollServiceOperationsTask").displayName("Poll Service Operations Step").description(
            "Poll Service Operations Step").build();
    }

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws Exception {
        logActivitiTask(context, LOGGER);
        try {
            debug(context, Messages.WAITING_FOR_SERVICES_CREATION, LOGGER);

            ClientExtensions clientExtensions = getClientExtensions(context, LOGGER);
            if (clientExtensions != null) {
                // The asynchronous creation of services is not supported yet on XSA.
                return ExecutionStatus.SUCCESS;
            }

            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);

            List<CloudServiceExtended> services = StepsUtil.getServicesToCreate(context);

            Map<CloudServiceExtended, ServiceOperation> servicesWithLastOperation = new HashMap<>();
            for (CloudServiceExtended service : services) {
                if (service.isUserProvided()) {
                    // Do not check the service instances of user provided service because
                    // the operation of creating such service is synchronous
                    continue;
                }
                ServiceOperation lastServiceOperation = getLastServiceOperation(context, client, service);
                if (lastServiceOperation == null) {
                    continue;
                }
                servicesWithLastOperation.put(service, lastServiceOperation);
            }

            return determineState(context, servicesWithLastOperation);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, Messages.ERROR_MONITORING_CREATION_OF_SERVICES, e, LOGGER);
            throw e;
        } catch (SLException e) {
            error(context, Messages.ERROR_MONITORING_CREATION_OF_SERVICES, e, LOGGER);
            throw e;
        }
    }

    private ExecutionStatus determineState(DelegateExecution context,
        Map<CloudServiceExtended, ServiceOperation> servicesWithLastOperation) {
        for (Entry<CloudServiceExtended, ServiceOperation> serviceCreationState : servicesWithLastOperation.entrySet()) {
            CloudServiceExtended service = serviceCreationState.getKey();
            ServiceOperation lastOperation = serviceCreationState.getValue();

            info(context, MessageFormat.format(Messages.WAITING_FOR_SERVICE_TO_BE_CREATED, service.getName()), LOGGER);

            if (lastOperation.getState() == ServiceOperationState.SUCCEEDED) {
                info(context, MessageFormat.format(Messages.SERVICE_CREATED, service.getName()), LOGGER);
                continue;
            }

            if (lastOperation.getState() == ServiceOperationState.FAILED) {
                if (!service.isOptional()) {
                    throw new SLException(
                        MessageFormat.format(Messages.ERROR_CREATING_SERVICE_WITH_NAME, service.getName(), lastOperation.getDescription()));
                }
            }

            if (lastOperation.getState() == ServiceOperationState.IN_PROGRESS) {
                return ExecutionStatus.RUNNING;
            }
        }
        return ExecutionStatus.SUCCESS;
    }

    private ServiceOperation getLastServiceOperation(DelegateExecution context, CloudFoundryOperations client,
        CloudServiceExtended service) {
        Map<String, Object> cloudServiceInstance = serviceOperationExecutor.executeServiceOperation(service, () -> {
            return serviceInstanceGetter.getServiceInstance(client, service.getName(), StepsUtil.getSpaceId(context));
        });

        validateCloudServiceInstance(context, service, cloudServiceInstance);
        if (cloudServiceInstance == null) {
            return null;
        }

        return getLastOperation(context, cloudServiceInstance);
    }

    private void validateCloudServiceInstance(DelegateExecution context, CloudServiceExtended service,
        Map<String, Object> cloudServiceInstance) {
        if (cloudServiceInstance == null && !service.isOptional()) {
            throw new SLException(MessageFormat.format(Messages.CANNOT_RETRIEVE_INSTANCE_OF_SERVICE, service.getName()));
        }

        if (cloudServiceInstance == null && service.isOptional()) {
            warn(context, MessageFormat.format(Messages.CANNOT_RETRIEVE_SERVICE_INSTANCE_OF_OPTIONAL_SERVICE, service.getName()), LOGGER);
        }
    }

    @SuppressWarnings("unchecked")
    private ServiceOperation getLastOperation(DelegateExecution context, Map<String, Object> cloudServiceInstance) {
        Map<String, Object> lastOperationAsMap = (Map<String, Object>) cloudServiceInstance.get(LAST_SERVICE_OPERATION);
        ServiceOperation lastOperation = parseServiceOperationFromMap(lastOperationAsMap);
        // TODO Separate create and update steps
        // Be fault tolerant on failure on update of service
        if (lastOperation.getType() == ServiceOperationType.UPDATE && lastOperation.getState() == ServiceOperationState.FAILED) {
            warn(context, MessageFormat.format(Messages.FAILED_SERVICE_UPDATE, cloudServiceInstance.get(SERVICE_NAME),
                lastOperation.getDescription()), LOGGER);
            return new ServiceOperation(lastOperation.getType(), lastOperation.getDescription(), ServiceOperationState.SUCCEEDED);
        }
        return lastOperation;
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_SERVICES_TO_CREATE_COUNT;
    }

    @Override
    public String getLogicalStepName() {
        return CreateOrUpdateServicesStep.class.getSimpleName();
    }

    private ServiceOperation parseServiceOperationFromMap(Map<String, Object> serviceOperation) {
        ServiceOperationType type = ServiceOperationType.fromString((String) serviceOperation.get(SERVICE_OPERATION_TYPE));
        ServiceOperationState state = ServiceOperationState.fromString((String) serviceOperation.get(SERVICE_OPERATION_STATE));
        String description = (String) serviceOperation.get(SERVICE_OPERATION_DESCRIPTION);
        if (description == null && state == ServiceOperationState.FAILED) {
            description = Messages.DEFAULT_FAILED_OPERATION_DESCRIPTION;
        }
        return new ServiceOperation(type, description, state);
    }

}

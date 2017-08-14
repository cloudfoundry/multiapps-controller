package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("deleteServicesStep")
public class DeleteServicesStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteServicesStep.class);

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("deleteServicesTask").displayName("Delete Discontinued Services").description(
            "Delete Discontinued Services").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.DELETING_SERVICES, LOGGER);

            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);

            List<String> servicesToDelete = StepsUtil.getServicesToDelete(context);

            deleteServices(context, client, servicesToDelete);

            debug(context, Messages.SERVICES_DELETED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_DELETING_SERVICES, e, LOGGER);
            throw e;
        } catch (CloudFoundryException e) {
            SLException ex = StepsUtil.createException(e);
            error(context, Messages.ERROR_DELETING_SERVICES, ex, LOGGER);
            throw ex;
        }
    }

    private void deleteServices(DelegateExecution context, CloudFoundryOperations client, List<String> serviceNames) {
        for (String serviceName : serviceNames) {
            attemptToDeleteService(context, client, serviceName);
        }
    }

    private void attemptToDeleteService(DelegateExecution context, CloudFoundryOperations client, String serviceName) {
        try {
            deleteService(context, client, serviceName);
        } catch (CloudFoundryException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                warn(context, format(Messages.COULD_NOT_DELETE_SERVICE, serviceName), e, LOGGER);
                return;
            }
            throw e;
        }
    }

    private void deleteService(DelegateExecution context, CloudFoundryOperations client, String serviceName) {
        CloudServiceInstance serviceInstance = client.getServiceInstance(serviceName);
        // Instead of throwing an exception, the CF client returns null if there is no service instance with the specified name.
        if (serviceInstance == null) {
            warn(context, format(Messages.COULD_NOT_DELETE_SERVICE, serviceName), LOGGER);
            return;
        }
        List<CloudServiceBinding> bindings = serviceInstance.getBindings();
        if (bindings != null && bindings.size() > 0) {
            logBindings(context, bindings);
            info(context, format(Messages.SERVICE_HAS_BINDINGS_AND_CANNOT_BE_DELETED, serviceName), LOGGER);
            return;
        }

        info(context, format(Messages.DELETING_SERVICE, serviceName), LOGGER);
        client.deleteService(serviceName);
        debug(context, format(Messages.SERVICE_DELETED, serviceName), LOGGER);
    }

    private void logBindings(DelegateExecution context, List<CloudServiceBinding> bindings) {
        debug(context, format(Messages.SERVICE_BINDINGS_EXISTS, secureSerializer.toJson(bindings)), LOGGER);
    }

}

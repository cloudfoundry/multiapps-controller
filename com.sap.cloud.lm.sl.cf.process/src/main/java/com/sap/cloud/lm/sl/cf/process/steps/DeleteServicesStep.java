package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.function.Function;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("deleteServicesStep")
public class DeleteServicesStep extends AbstractXS2ProcessStep {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteServicesStep.class);

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    public static StepMetadata getMetadata() {
        return new StepMetadata("deleteServicesTask", "Delete Discontinued Services", "Delete Discontinued Services");
    }

    protected Function<DelegateExecution, CloudFoundryOperations> clientSupplier = (context) -> getCloudFoundryClient(context, LOGGER);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.DELETING_SERVICES, LOGGER);

            CloudFoundryOperations client = clientSupplier.apply(context);

            List<String> servicesToDelete = StepsUtil.getServicesToDelete(context);

            boolean deleteAllowed = (Boolean) context.getVariable(Constants.PARAM_DELETE_SERVICES);
            if (deleteAllowed) {
                deleteServices(context, client, servicesToDelete);
            }

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

    private void deleteServices(DelegateExecution context, CloudFoundryOperations client, List<String> services) {
        for (String service : services) {
            deleteService(context, client, service);
        }
    }

    private void deleteService(DelegateExecution context, CloudFoundryOperations client, String service) {
        List<CloudServiceBinding> bindings = client.getServiceInstance(service).getBindings();
        if (bindings != null && bindings.size() > 0) {
            logBindings(context, bindings);
            info(context, format(Messages.SERVICE_HAS_BINDINGS_AND_CANNOT_BE_DELETED, service), LOGGER);
            return;
        }

        info(context, format(Messages.DELETING_SERVICE, service), LOGGER);
        client.deleteService(service);
        debug(context, format(Messages.SERVICE_DELETED, service), LOGGER);
    }

    private void logBindings(DelegateExecution context, List<CloudServiceBinding> bindings) {
        debug(context, format(Messages.SERVICE_BINDINGS_EXISTS, secureSerializer.toJson(bindings)), LOGGER);
    }

}

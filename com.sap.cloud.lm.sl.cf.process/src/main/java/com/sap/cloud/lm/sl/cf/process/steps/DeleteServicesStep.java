package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("deleteServicesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteServicesStep extends SyncActivitiStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.DELETING_SERVICES);

            CloudControllerClient client = execution.getControllerClient();

            List<String> servicesToDelete = StepsUtil.getServicesToDelete(execution.getContext());
            deleteServices(client, servicesToDelete);

            getStepLogger().debug(Messages.SERVICES_DELETED);
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DELETING_SERVICES);
            throw e;
        }
    }

    private void deleteServices(CloudControllerClient client, List<String> serviceNames) {
        for (String serviceName : serviceNames) {
            deleteService(client, serviceName);
        }
    }

    private void deleteService(CloudControllerClient client, String serviceName) {
        CloudServiceInstance serviceInstance = null;
        try {
            serviceInstance = client.getServiceInstance(serviceName);
            attemptToDeleteService(client, serviceInstance, serviceName);
        } catch (CloudOperationException | CloudException e) {
            processException(e, serviceInstance, serviceName);
        }
    }

    private void attemptToDeleteService(CloudControllerClient client, CloudServiceInstance serviceInstance, String serviceName) {
        List<CloudServiceBinding> bindings = serviceInstance.getBindings();
        List<CloudApplication> applications = client.getApplications();

        if (!CollectionUtils.isEmpty(bindings)) {
            logBindings(bindings);
            for (CloudApplication application : applications) {
                unbindServiceFromApplication(client, application, serviceName);
            }
        }

        List<ServiceKey> serviceKeys = client.getServiceKeys(serviceName);
        for (ServiceKey serviceKey : serviceKeys) {
            deleteServiceKeyFromService(client, serviceName, serviceKey);
        }

        getStepLogger().info(Messages.DELETING_SERVICE, serviceName);
        client.deleteService(serviceName);
        getStepLogger().debug(Messages.SERVICE_DELETED, serviceName);
    }

    private void deleteServiceKeyFromService(CloudControllerClient client, String serviceName, ServiceKey serviceKey) {
        getStepLogger().info(Messages.DELETING_SERVICE_KEY_FOR_SERVICE, serviceKey, serviceName);
        client.deleteServiceKey(serviceName, serviceKey.getName());
    }

    private void unbindServiceFromApplication(CloudControllerClient client, CloudApplication application, String serviceName) {
        getStepLogger().info(Messages.UNBINDING_APP_FROM_SERVICE, application.getName(), serviceName);
        client.unbindService(application.getName(), serviceName);
    }

    private void processException(Exception e, CloudServiceInstance serviceInstance, String serviceName) {
        if (e instanceof CloudOperationException) {
            e = evaluateCloudOperationException((CloudOperationException) e, serviceName);
            if (e == null) {
                return;
            }
        }
        wrapAndThrowException(e, serviceInstance, serviceName);
    }

    private CloudOperationException evaluateCloudOperationException(CloudOperationException e, String serviceName) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            getStepLogger().warn(e, Messages.COULD_NOT_DELETE_SERVICE, serviceName);
            return null;
        }
        if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
            return new CloudServiceBrokerException(e);
        }
        return new CloudControllerException(e);

    }

    private void wrapAndThrowException(Exception e, CloudServiceInstance serviceInstance, String serviceName) {
        String msg = buildNewExceptionMessage(e, serviceInstance, serviceName);
        throw new SLException(e, msg);
    }

    private String buildNewExceptionMessage(Exception e, CloudServiceInstance serviceInstance, String serviceName) {
        if (serviceInstance == null) {
            return MessageFormat.format(Messages.ERROR_DELETING_SERVICE_SHORT, serviceName, e.getMessage());
        }
        CloudService service = serviceInstance.getService();
        return MessageFormat.format(Messages.ERROR_DELETING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
            e.getMessage());
    }

    private void logBindings(List<CloudServiceBinding> bindings) {
        getStepLogger().debug(Messages.SERVICE_BINDINGS_EXISTS, secureSerializer.toJson(bindings));
    }

}

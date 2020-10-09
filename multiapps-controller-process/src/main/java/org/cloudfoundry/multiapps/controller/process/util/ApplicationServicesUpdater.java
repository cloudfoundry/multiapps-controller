package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.ApplicationServicesUpdateCallback;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.springframework.http.HttpStatus;

public class ApplicationServicesUpdater extends ControllerClientFacade {

    public ApplicationServicesUpdater(Context context) {
        super(context);
    }

    public List<String> updateApplicationServices(String applicationName,
                                                  Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                                                  ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        CloudApplication application = getControllerClient().getApplication(applicationName);

        List<String> addServices = calculateServicesToAdd(serviceNamesWithBindingParameters.keySet(), application);
        bindServices(addServices, applicationName, serviceNamesWithBindingParameters, applicationServicesUpdateCallback);

        List<String> deleteServices = calculateServicesToDelete(serviceNamesWithBindingParameters.keySet(), application);
        unbindServices(deleteServices, applicationName, applicationServicesUpdateCallback);

        List<String> rebindServices = calculateServicesToRebind(serviceNamesWithBindingParameters, application);
        rebindServices(rebindServices, applicationName, serviceNamesWithBindingParameters, applicationServicesUpdateCallback);

        return getUpdatedServiceNames(addServices, deleteServices, rebindServices);
    }

    private List<String> calculateServicesToAdd(Set<String> services, CloudApplication application) {
        return services.stream()
                       .filter(serviceName -> !application.getServices()
                                                          .contains(serviceName))
                       .collect(Collectors.toList());
    }

    private List<String> calculateServicesToDelete(Set<String> services, CloudApplication application) {
        return application.getServices()
                          .stream()
                          .filter(serviceName -> !services.contains(serviceName))
                          .collect(Collectors.toList());
    }

    private List<String> calculateServicesToRebind(Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                                                   CloudApplication application) {
        List<String> servicesToRebind = new ArrayList<>();
        for (String serviceName : serviceNamesWithBindingParameters.keySet()) {
            if (!application.getServices()
                            .contains(serviceName)) {
                continue;
            }

            CloudServiceInstance serviceInstance = getControllerClient().getServiceInstance(serviceName);
            Map<String, Object> newServiceBindingParameters = getNewServiceBindingParameters(serviceNamesWithBindingParameters,
                                                                                             serviceInstance);
            if (hasServiceBindingChanged(application, serviceInstance, newServiceBindingParameters)) {
                servicesToRebind.add(serviceInstance.getName());
            }
        }
        return servicesToRebind;
    }

    private Map<String, Object> getNewServiceBindingParameters(Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                                                               CloudServiceInstance serviceInstance) {
        return serviceNamesWithBindingParameters.get(serviceInstance.getName());
    }

    private boolean hasServiceBindingChanged(CloudApplication application, CloudServiceInstance serviceInstance,
                                             Map<String, Object> newServiceBindingParameters) {
        Map<String, Object> currentBindingParameters = getServiceBindingParameters(application, serviceInstance);
        return !Objects.equals(currentBindingParameters, newServiceBindingParameters);
    }

    private Map<String, Object> getServiceBindingParameters(CloudApplication application, CloudServiceInstance serviceInstance) {
        CloudServiceBinding serviceBinding = getServiceBinding(application, serviceInstance);
        try {
            return getControllerClient().getServiceBindingParameters(getGuid(serviceBinding));
        } catch (CloudOperationException e) {
            if (HttpStatus.NOT_IMPLEMENTED == e.getStatusCode() || HttpStatus.BAD_REQUEST == e.getStatusCode()) {
                getLogger().warnWithoutProgressMessage(Messages.CANNOT_RETRIEVE_PARAMETERS_OF_BINDING_BETWEEN_APPLICATION_0_AND_SERVICE_INSTANCE_1,
                                                       application.getName(), serviceInstance.getName());
                return null;
            }
            throw e;
        }
    }

    private CloudServiceBinding getServiceBinding(CloudApplication application, CloudServiceInstance serviceInstance) {
        List<CloudServiceBinding> serviceBindings = getControllerClient().getServiceBindings(getGuid(serviceInstance));
        getLogger().debug(Messages.LOOKING_FOR_SERVICE_BINDINGS, getGuid(application), getGuid(serviceInstance),
                          SecureSerialization.toJson(serviceBindings));
        for (CloudServiceBinding serviceBinding : serviceBindings) {
            if (application.getMetadata()
                           .getGuid()
                           .equals(serviceBinding.getApplicationGuid())) {
                return serviceBinding;
            }
        }
        throw new IllegalStateException(MessageFormat.format(Messages.APPLICATION_UNBOUND_IN_PARALLEL, application.getName(),
                                                             serviceInstance.getName()));
    }

    private void bindServices(List<String> addServices, String applicationName,
                              Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                              ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        for (String serviceName : addServices) {
            Map<String, Object> bindingParameters = serviceNamesWithBindingParameters.get(serviceName);
            getControllerClient().bindServiceInstance(applicationName, serviceName, bindingParameters, applicationServicesUpdateCallback);
        }
    }

    private void unbindServices(List<String> deleteServices, String applicationName,
                                ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        for (String serviceName : deleteServices) {
            getControllerClient().unbindServiceInstance(applicationName, serviceName, applicationServicesUpdateCallback);
        }
    }

    private void rebindServices(List<String> rebindServices, String applicationName,
                                Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                                ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        for (String serviceName : rebindServices) {
            Map<String, Object> bindingParameters = serviceNamesWithBindingParameters.get(serviceName);
            getControllerClient().unbindServiceInstance(applicationName, serviceName, applicationServicesUpdateCallback);
            getControllerClient().bindServiceInstance(applicationName, serviceName, bindingParameters, applicationServicesUpdateCallback);
        }
    }

    private List<String> getUpdatedServiceNames(List<String> addServices, List<String> deleteServices, List<String> rebindServices) {
        List<String> result = new ArrayList<>();
        result.addAll(addServices);
        result.addAll(deleteServices);
        result.addAll(rebindServices);
        return result;
    }

    private UUID getGuid(CloudEntity entity) {
        return entity.getMetadata()
                     .getGuid();
    }
}

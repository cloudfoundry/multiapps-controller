package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.ApplicationServicesUpdateCallback;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.util.JsonUtil;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.process.Messages;

public class ApplicationServicesUpdater {

    private CloudControllerClient client;
    private StepLogger stepLogger;

    public ApplicationServicesUpdater(CloudControllerClient client, StepLogger stepLogger) {
        this.client = client;
        this.stepLogger = stepLogger;
    }

    public List<String> updateApplicationServices(String applicationName,
                                                  Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                                                  ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        CloudApplication application = client.getApplication(applicationName);

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

    protected List<String> calculateServicesToRebind(Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                                                     CloudApplication application) {
        List<String> servicesToRebind = new ArrayList<>();
        for (String serviceName : serviceNamesWithBindingParameters.keySet()) {
            if (!application.getServices()
                            .contains(serviceName)) {
                continue;
            }

            CloudServiceInstance serviceInstance = client.getServiceInstance(serviceName);
            Map<String, Object> newServiceBindingParameters = getNewServiceBindingParameters(serviceNamesWithBindingParameters,
                                                                                             serviceInstance);
            if (hasServiceBindingsChanged(application, serviceInstance, newServiceBindingParameters)) {
                servicesToRebind.add(serviceInstance.getService()
                                                    .getName());
            }
        }
        return servicesToRebind;
    }

    private Map<String, Object> getNewServiceBindingParameters(Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                                                               CloudServiceInstance serviceInstance) {
        return serviceNamesWithBindingParameters.get(serviceInstance.getService()
                                                                    .getName());
    }

    private boolean hasServiceBindingsChanged(CloudApplication application, CloudServiceInstance serviceInstance,
                                              Map<String, Object> newServiceBindingParameters) {
        CloudServiceBinding bindingForApplication = getServiceBindingForApplication(application, serviceInstance);
        return !Objects.equals(bindingForApplication.getBindingParameters(), newServiceBindingParameters);
    }

    private CloudServiceBinding getServiceBindingForApplication(CloudApplication application, CloudServiceInstance serviceInstance) {
        stepLogger.debug(Messages.LOOKING_FOR_SERVICE_BINDINGS, getGuid(application), getGuid(serviceInstance),
                         JsonUtil.convertToJson(serviceInstance.getBindings(), true));
        return serviceInstance.getBindings()
                              .stream()
                              .filter(serviceBinding -> application.getMetadata()
                                                                   .getGuid()
                                                                   .equals(serviceBinding.getApplicationGuid()))
                              .findFirst()
                              .orElseThrow(() -> new IllegalStateException(MessageFormat.format(Messages.APPLICATION_UNBOUND_IN_PARALLEL,
                                                                                                application.getName(),
                                                                                                serviceInstance.getService()
                                                                                                               .getName())));
    }

    private void bindServices(List<String> addServices, String applicationName,
                              Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                              ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        for (String serviceName : addServices) {
            Map<String, Object> bindingParameters = serviceNamesWithBindingParameters.get(serviceName);
            stepLogger.info(Messages.BINDING_APP_TO_SERVICE_WITH_PARAMETERS, applicationName, serviceName, bindingParameters);
            client.bindService(applicationName, serviceName, bindingParameters, applicationServicesUpdateCallback);
        }
    }

    private void unbindServices(List<String> deleteServices, String applicationName,
                                ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        for (String serviceName : deleteServices) {
            stepLogger.info(Messages.UNBINDING_APP_FROM_SERVICE, applicationName, serviceName);
            client.unbindService(applicationName, serviceName, applicationServicesUpdateCallback);
        }
    }

    private void rebindServices(List<String> rebindServices, String applicationName,
        Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
        ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        for (String serviceName : rebindServices) {
            Map<String, Object> bindingParameters = serviceNamesWithBindingParameters.get(serviceName);
            stepLogger.info(Messages.UNBINDING_APP_FROM_SERVICE, applicationName, serviceName);
            client.unbindService(applicationName, serviceName, applicationServicesUpdateCallback);
            stepLogger.info(Messages.BINDING_APP_TO_SERVICE_WITH_PARAMETERS, applicationName, serviceName, bindingParameters);
            client.bindService(applicationName, serviceName, bindingParameters, applicationServicesUpdateCallback);
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

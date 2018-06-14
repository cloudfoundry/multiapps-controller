package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.springframework.web.client.RestTemplate;

public class ServiceBindingCreator extends CustomControllerClient {

    private static final String SERVICE_BINDINGS_ENDPOINT = "/v2/service_bindings";
    private static final String SERVICE_BINDINGS_PARAMETERS = "parameters";
    private static final String SERVICE_INSTANCE_GUID = "service_instance_guid";
    private static final String APP_GUID = "app_guid";

    @Inject
    public ServiceBindingCreator(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    public void bindService(CloudFoundryOperations client, String appName, String serviceName, Map<String, Object> parameters) {
        new CustomControllerClientErrorHandler().handleErrors(() -> attemptToBindService(client, appName, serviceName, parameters));
    }

    private void attemptToBindService(CloudFoundryOperations client, String appName, String serviceName, Map<String, Object> parameters) {
        String serviceBindingsUrl = getUrl(client.getCloudControllerUrl()
            .toString(), SERVICE_BINDINGS_ENDPOINT);
        CloudApplication cloudApplication = client.getApplication(appName);
        UUID appGuid = cloudApplication.getMeta()
            .getGuid();
        CloudService cloudService = client.getService(serviceName);
        UUID serviceGuid = cloudService.getMeta()
            .getGuid();
        Map<String, Object> request = createServiceBindingRequest(appGuid, serviceGuid, parameters);
        RestTemplate restTemplate = getRestTemplate(client);
        restTemplate.postForObject(serviceBindingsUrl, request, String.class);
    }

    private Map<String, Object> createServiceBindingRequest(UUID appGuid, UUID serviceInstanceGuid, Map<String, Object> parameters) {
        Map<String, Object> serviceBindingsRequest = new HashMap<>();
        serviceBindingsRequest.put(SERVICE_INSTANCE_GUID, serviceInstanceGuid);
        serviceBindingsRequest.put(APP_GUID, appGuid);
        serviceBindingsRequest.put(SERVICE_BINDINGS_PARAMETERS, parameters);
        return serviceBindingsRequest;
    }

    protected CloudEntityResourceMapper getResourceMapper() {
        return new CloudEntityResourceMapper();
    }

}

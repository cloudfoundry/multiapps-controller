package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

public class ServiceUpdater extends CloudServiceOperator {

    private static final String USER_PROVIDED_SERVICE_INSTANCES_URL = "/v2/user_provided_service_instances";
    private static final String SERVICE_CREDENTIALS = "credentials";
    private static final String ACCEPTS_INCOMPLETE_TRUE = "?accepts_incomplete=true";
    private static final String SERVICE_PARAMETERS = "parameters";

    public void updateServicePlan(CloudFoundryOperations client, String serviceName, String servicePlan) {
        new CustomControllerClientErrorHandler().handleErrorsAndWarnings(
            () -> attemptToUpdateServicePlan(client, serviceName, servicePlan));
    }

    private void attemptToUpdateServicePlan(CloudFoundryOperations client, String serviceName, String servicePlan) {
        CloudService service = getCloudService(client, serviceName);

        RestTemplate restTemplate = getRestTemplate(client);
        String cloudControllerUrl = getCloudControllerUrl(client);

        String servicePlanGuid = findPlanForService(service, servicePlan, restTemplate, cloudControllerUrl).getMeta().getGuid().toString();
        attemptToUpdateServiceParameter(SERVICE_PLAN_GUID, servicePlanGuid, restTemplate, getUrl(cloudControllerUrl,
            getUpdateServiceUrl(SERVICE_INSTANCES_URL, service.getMeta().getGuid().toString(), ACCEPTS_INCOMPLETE_TRUE)));
    }

    private String getCloudControllerUrl(CloudFoundryOperations client) {
        return client.getCloudControllerUrl().toString();
    }

    private CloudService getCloudService(CloudFoundryOperations client, String serviceName) {
        CloudService service = client.getService(serviceName);
        if (service == null) {
            throw new CloudFoundryException(HttpStatus.NOT_FOUND, "Not Found", "Service '" + serviceName + "' not found");
        }
        return service;
    }

    public void updateServiceParameters(CloudFoundryOperations client, String serviceName, Map<String, Object> parameters) {
        new CustomControllerClientErrorHandler().handleErrorsAndWarnings(
            () -> attemptToUpdateServiceParameters(client, serviceName, parameters));
    }

    private void attemptToUpdateServiceParameters(CloudFoundryOperations client, String serviceName, Map<String, Object> parameters) {
        assertServiceAttributes(serviceName, parameters);
        CloudService service = getCloudService(client, serviceName);

        RestTemplate restTemplate = getRestTemplate(client);
        String cloudControllerUrl = getCloudControllerUrl(client);

        if (service.isUserProvided()) {
            attemptToUpdateServiceParameter(SERVICE_CREDENTIALS, parameters, restTemplate,
                getUrl(cloudControllerUrl, getUpdateServiceUrl(USER_PROVIDED_SERVICE_INSTANCES_URL, service.getMeta().getGuid().toString(),
                    ACCEPTS_INCOMPLETE_FALSE)));
            return;
        }

        attemptToUpdateServiceParameter(SERVICE_PARAMETERS, parameters, restTemplate, getUrl(cloudControllerUrl,
            getUpdateServiceUrl(SERVICE_INSTANCES_URL, service.getMeta().getGuid().toString(), ACCEPTS_INCOMPLETE_FALSE)));
    }

    private void attemptToUpdateServiceParameter(String parameterName, Object parameter, RestTemplate restTemplate,
        String updateServiceUrl) {
        Map<String, Object> serviceRequest = createUpdateServiceRequest(parameterName, parameter);
        restTemplate.put(updateServiceUrl, serviceRequest);
    }

    private Map<String, Object> createUpdateServiceRequest(String requestParameter, Object parameter) {
        Map<String, Object> updateServiceParametersRequest = new HashMap<String, Object>();
        updateServiceParametersRequest.put(requestParameter, parameter);
        return updateServiceParametersRequest;
    }

    private String getUpdateServiceUrl(String serviceUrl, String serviceGuid, String acceptsIncomplete) {
        return serviceUrl + "/" + serviceGuid + acceptsIncomplete;
    }

    private void assertServiceAttributes(String serviceName, Object parameters) {
        Assert.notNull(serviceName, "Service name must not be null");
        Assert.notNull(parameters, "Service parameters must not be null");
    }
}

package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;

public class ServiceCreator extends CloudServiceOperator {

    public void createService(CloudFoundryOperations client, CloudServiceExtended service, String spaceId) {
        new CustomControllerClientErrorHandler().handleErrors(() -> attemptToCreateService(client, service, spaceId));
    }

    private void attemptToCreateService(CloudFoundryOperations client, CloudServiceExtended service, String spaceId) {
        assertServiceAttributes(service);

        RestTemplate restTemplate = getRestTemplate(client);
        String cloudControllerUrl = client.getCloudControllerUrl().toString();
        CloudServicePlan cloudServicePlan = findPlanForService(service, restTemplate, cloudControllerUrl);

        Map<String, Object> serviceRequest = createServiceRequest(service, spaceId, cloudServicePlan);
        restTemplate.postForObject(getUrl(cloudControllerUrl, CREATE_SERVICE_URL_ACCEPTS_INCOMPLETE_FALSE), serviceRequest, String.class);
    }

    private Map<String, Object> createServiceRequest(CloudServiceExtended service, String spaceId, CloudServicePlan cloudServicePlan) {
        Map<String, Object> serviceRequest = new HashMap<String, Object>();
        serviceRequest.put(SPACE_GUID, spaceId);
        serviceRequest.put(SERVICE_NAME, service.getName());
        serviceRequest.put(SERVICE_PLAN_GUID, cloudServicePlan.getMeta().getGuid());
        serviceRequest.put(SERVICE_PARAMETERS, service.getCredentials());
        return serviceRequest;
    }

    private void assertServiceAttributes(CloudServiceExtended service) {
        Assert.notNull(service, "Service must not be null");
        Assert.notNull(service.getName(), "Service name must not be null");
        Assert.notNull(service.getLabel(), "Service label must not be null");
        Assert.notNull(service.getPlan(), "Service plan must not be null");
    }
}

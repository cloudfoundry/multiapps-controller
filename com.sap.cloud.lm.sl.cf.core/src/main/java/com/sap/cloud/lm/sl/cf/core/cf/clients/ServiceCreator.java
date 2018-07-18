package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;

public class ServiceCreator extends CloudServiceOperator {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ServiceCreator.class);

    @Inject
    public ServiceCreator(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    public void createService(CloudControllerClient client, CloudServiceExtended service, String spaceId) {
        new CustomControllerClientErrorHandler().handleErrors(() -> attemptToCreateService(client, service, spaceId));
    }

    private void attemptToCreateService(CloudControllerClient client, CloudServiceExtended service, String spaceId) {
        assertServiceAttributes(service);

        RestTemplate restTemplate = getRestTemplate(client);
        String cloudControllerUrl = client.getCloudControllerUrl()
            .toString();
        CloudServicePlan cloudServicePlan = findPlanForService(client, service);

        Map<String, Object> serviceRequest = createServiceRequest(service, spaceId, cloudServicePlan);
        restTemplate.postForObject(getUrl(cloudControllerUrl, CREATE_SERVICE_URL_ACCEPTS_INCOMPLETE_TRUE), serviceRequest, String.class);
    }

    private Map<String, Object> createServiceRequest(CloudServiceExtended service, String spaceId, CloudServicePlan cloudServicePlan) {
        Map<String, Object> serviceRequest = new HashMap<>();
        serviceRequest.put(SPACE_GUID, spaceId);
        serviceRequest.put(SERVICE_NAME, service.getName());
        serviceRequest.put(SERVICE_PLAN_GUID, cloudServicePlan.getMeta()
            .getGuid()
            .toString());
        serviceRequest.put(SERVICE_PARAMETERS, service.getCredentials());
        serviceRequest.put(SERVICE_TAGS, service.getTags());
        return serviceRequest;
    }

    private void assertServiceAttributes(CloudServiceExtended service) {
        Assert.notNull(service, "Service must not be null");
        Assert.notNull(service.getName(), "Service name must not be null");
        Assert.notNull(service.getLabel(), "Service label must not be null");
        Assert.notNull(service.getPlan(), "Service plan must not be null");
    }
}

package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;

public class ServiceCreator extends CloudServiceOperator {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ServiceCreator.class);

    @Inject
    public ServiceCreator(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    public MethodExecution<String> createService(CloudControllerClient client, CloudServiceExtended service, String spaceId) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> attemptToCreateService(client, service, spaceId));
    }

    private MethodExecution<String> attemptToCreateService(CloudControllerClient client, CloudServiceExtended service, String spaceId) {
        assertServiceAttributes(service);

        RestTemplate restTemplate = getRestTemplate(client);
        String cloudControllerUrl = client.getCloudControllerUrl()
            .toString();
        CloudServicePlan cloudServicePlan = findPlanForService(client, service);

        Map<String, Object> serviceRequest = createServiceRequest(service, spaceId, cloudServicePlan);
        String url = getUrl(cloudControllerUrl, CREATE_SERVICE_URL_ACCEPTS_INCOMPLETE_TRUE);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(serviceRequest);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        return MethodExecution.fromResponseEntity(response);
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

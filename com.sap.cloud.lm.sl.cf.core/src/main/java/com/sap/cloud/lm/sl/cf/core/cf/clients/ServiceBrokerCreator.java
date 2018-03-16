package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceBrokerExtended;

public class ServiceBrokerCreator extends CustomControllerClient {

    private static final String SERVICE_BROKERS_URL = "/v2/service_brokers";

    @Inject
    public ServiceBrokerCreator(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    public void createServiceBroker(CloudFoundryOperations client, CloudServiceBrokerExtended serviceBroker) {
        new CustomControllerClientErrorHandler().handleErrors(() -> attemptToCreateServiceBroker(client, serviceBroker));
    }

    private void attemptToCreateServiceBroker(CloudFoundryOperations client, CloudServiceBrokerExtended serviceBroker) {
        validateEntity(serviceBroker);
        Map<String, Object> serviceBrokerCreationRequest = getServiceBrokerCreationRequest(serviceBroker);
        String controllerUrl = client.getCloudControllerUrl()
            .toString();
        RestTemplate restTemplate = getRestTemplate(client);
        restTemplate.postForObject(getUrl(controllerUrl, SERVICE_BROKERS_URL), serviceBrokerCreationRequest, String.class);
    }

    private Map<String, Object> getServiceBrokerCreationRequest(CloudServiceBrokerExtended serviceBroker) {
        TreeMap<String, Object> serviceBrokerCreationRequest = new TreeMap<>();
        serviceBrokerCreationRequest.put("name", serviceBroker.getName());
        serviceBrokerCreationRequest.put("broker_url", serviceBroker.getUrl());
        serviceBrokerCreationRequest.put("auth_username", serviceBroker.getUsername());
        serviceBrokerCreationRequest.put("auth_password", serviceBroker.getPassword());
        serviceBrokerCreationRequest.put("space_guid", serviceBroker.getSpaceGuid());
        return serviceBrokerCreationRequest;
    }

    private void validateEntity(CloudServiceBrokerExtended serviceBroker) {
        Assert.notNull(serviceBroker, "The service broker must not be null!");
        Assert.notNull(serviceBroker.getUrl(), "The service broker's URL must not be null!");
        Assert.notNull(serviceBroker.getName(), "The service broker's name must not be null!");
        Assert.notNull(serviceBroker.getPassword(), "The service broker's password must not be null!");
        Assert.notNull(serviceBroker.getUsername(), "The service broker's username must not be null!");
    }

}

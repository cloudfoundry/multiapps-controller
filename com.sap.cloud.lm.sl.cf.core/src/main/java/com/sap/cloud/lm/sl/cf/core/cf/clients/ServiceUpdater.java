package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

@Named("serviceUpdater")
@Profile("cf")
public class ServiceUpdater extends CloudServiceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUpdater.class);

    private static final String USER_PROVIDED_SERVICE_INSTANCES_URL = "/v2/user_provided_service_instances";
    private static final String SERVICE_CREDENTIALS = "credentials";
    private static final String ACCEPTS_INCOMPLETE_TRUE = "?accepts_incomplete=true";
    private static final String SERVICE_PARAMETERS = "parameters";

    @Inject
    public ServiceUpdater(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    public void updateServicePlanQuietly(CloudControllerClient client, String serviceName, String servicePlan) {
        ignoreBadGatewayErrors(() -> updateServicePlan(client, serviceName, servicePlan));
    }

    public void updateServicePlan(CloudControllerClient client, String serviceName, String servicePlan) {
        new CustomControllerClientErrorHandler().handleErrors(() -> attemptToUpdateServicePlan(client, serviceName, servicePlan));
    }

    public void updateServiceTagsQuietly(CloudControllerClient client, String serviceName, List<String> serviceTags) {
        ignoreBadGatewayErrors(() -> updateServiceTags(client, serviceName, serviceTags));
    }

    public void updateServiceTags(CloudControllerClient client, String serviceName, List<String> serviceTags) {
        new CustomControllerClientErrorHandler().handleErrors(() -> attemptToUpdateServiceTags(client, serviceName, serviceTags));
    }

    private void attemptToUpdateServicePlan(CloudControllerClient client, String serviceName, String servicePlanName) {
        CloudService service = client.getService(serviceName);

        CloudServicePlan servicePlan = findPlanForService(client, service, servicePlanName);
        String servicePlanGuid = servicePlan.getMeta()
                                            .getGuid()
                                            .toString();
        attemptToUpdateServiceParameter(client, serviceName, SERVICE_INSTANCES_URL, SERVICE_PLAN_GUID, servicePlanGuid);
    }

    private void attemptToUpdateServiceTags(CloudControllerClient client, String serviceName, List<String> serviceTags) {
        attemptToUpdateServiceParameter(client, serviceName, SERVICE_INSTANCES_URL, SERVICE_TAGS, serviceTags);
    }

    private String getCloudControllerUrl(CloudControllerClient client) {
        return client.getCloudControllerUrl()
                     .toString();
    }

    public void updateServiceParametersQuietly(CloudControllerClient client, String serviceName, Map<String, Object> parameters) {
        ignoreBadGatewayErrors(() -> updateServiceParameters(client, serviceName, parameters));
    }

    public void updateServiceParameters(CloudControllerClient client, String serviceName, Map<String, Object> parameters) {
        new CustomControllerClientErrorHandler().handleErrors(() -> attemptToUpdateServiceParameters(client, serviceName, parameters));
    }

    private void attemptToUpdateServiceParameters(CloudControllerClient client, String serviceName, Map<String, Object> parameters) {
        assertServiceAttributes(serviceName, parameters);
        CloudService service = client.getService(serviceName);

        if (service.isUserProvided()) {
            attemptToUpdateServiceParameter(client, serviceName, USER_PROVIDED_SERVICE_INSTANCES_URL, SERVICE_CREDENTIALS, parameters);
            return;
        }

        attemptToUpdateServiceParameter(client, serviceName, SERVICE_INSTANCES_URL, SERVICE_PARAMETERS, parameters);
    }

    private void attemptToUpdateServiceParameter(CloudControllerClient client, String serviceName, String serviceUrl, String parameterName,
                                                 Object parameter) {

        CloudService service = client.getService(serviceName);

        RestTemplate restTemplate = getRestTemplate(client);
        String cloudControllerUrl = getCloudControllerUrl(client);
        String updateServiceUrl = getUrl(cloudControllerUrl, getUpdateServiceUrl(serviceUrl, service.getMeta()
                                                                                                    .getGuid()
                                                                                                    .toString(),
                                                                                 ACCEPTS_INCOMPLETE_TRUE));

        Map<String, Object> serviceRequest = createUpdateServiceRequest(parameterName, parameter);
        restTemplate.put(updateServiceUrl, serviceRequest);
    }

    private Map<String, Object> createUpdateServiceRequest(String requestParameter, Object parameter) {
        Map<String, Object> updateServiceParametersRequest = new HashMap<>();
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

    private void ignoreBadGatewayErrors(Runnable runnable) {
        try {
            runnable.run();
        } catch (CloudOperationException e) {
            if (!e.getStatusCode()
                  .equals(HttpStatus.BAD_GATEWAY)) {
                throw e;
            }
            LOGGER.warn(MessageFormat.format("Controller operation failed. Status Code: {0}, Status Text: {1}, Description: {2}",
                                             e.getStatusCode(), e.getStatusText(), e.getDescription()));
        }
    }

}

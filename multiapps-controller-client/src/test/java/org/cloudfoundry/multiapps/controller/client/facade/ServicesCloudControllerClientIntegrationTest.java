package org.cloudfoundry.multiapps.controller.client.facade;

import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.APPLICATION_HOST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.jobs.JobState;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import org.cloudfoundry.multiapps.controller.client.facade.broker.FailConfiguration;
import org.cloudfoundry.multiapps.controller.client.facade.broker.ImmutableFailConfiguration;
import org.cloudfoundry.multiapps.controller.client.facade.broker.ImmutableServiceBrokerConfiguration;
import org.cloudfoundry.multiapps.controller.client.facade.broker.ServiceBrokerConfiguration;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ImmutableApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.util.JsonUtil;

class ServicesCloudControllerClientIntegrationTest extends CloudControllerClientIntegrationTest {

    private static final String SYSLOG_DRAIN_URL = "https://syslogDrain.com";
    private static final Map<String, Object> USER_SERVICE_CREDENTIALS = Map.of("testCredentialsKey", "testCredentialsValue");
    private static final String SERVICE_BROKER_ENDPOINT = "configurations/1";
    private static final List<String> SERVICE_TAGS = List.of("custom-tag-1", "custom-tag-2");

    private static boolean pushedServiceBroker = false;

    @BeforeAll
    static void setUp() throws InterruptedException {
        String brokerPathString = ITVariable.PATH_TO_SERVICE_BROKER_APPLICATION.getValue();
        if (brokerPathString == null) {
            return;
        }
        Path brokerPath = Paths.get(brokerPathString);
        if (Files.notExists(brokerPath)) {
            fail(MessageFormat.format("Specified service broker path \"{0}\" not exists", brokerPathString));
        }
        pushServiceBrokerApplication(brokerPath);
        try {
            createServiceBroker(IntegrationTestConstants.SERVICE_BROKER_NAME, SERVICE_BROKER_ENDPOINT);
            pushedServiceBroker = true;
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                //Ignore because service broker with this name exists
            } else {
                fail("Service broker creation failed!", e);
            }
        }
    }

    @AfterAll
    static void tearDown() {
        if (pushedServiceBroker) {
            String jobId = client.deleteServiceBroker(IntegrationTestConstants.SERVICE_BROKER_NAME);
            pollServiceBrokerOperation(jobId, IntegrationTestConstants.SERVICE_BROKER_NAME);
            client.deleteApplication(IntegrationTestConstants.SERVICE_BROKER_APP_NAME);
            client.deleteRoute(IntegrationTestConstants.SERVICE_BROKER_HOST, client.getDefaultDomain()
                                                                                   .getName(),
                               null);
        }
    }

    @Test
    @DisplayName("Create a user provided service and verify its parameters")
    void createUserProvidedServiceTest() {
        String serviceName = "test-service-1";
        try {
            client.createUserProvidedServiceInstance(buildUserProvidedService(serviceName));
            CloudServiceInstance service = client.getServiceInstance(serviceName);
            Map<String, Object> serviceCredentials = client.getUserProvidedServiceInstanceParameters(service.getGuid());
            assertEquals(SYSLOG_DRAIN_URL, service.getSyslogDrainUrl());
            assertEquals(USER_SERVICE_CREDENTIALS, serviceCredentials);
            assertEquals(SERVICE_TAGS, service.getTags());
            assertTrue(service.isUserProvided());
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteServiceInstance(serviceName);
        }
    }

    @Test
    @DisplayName("Create a user provided service and update its parameters")
    void updateUserProvidedServiceTest() {
        String serviceName = "test-service-2";
        Map<String, Object> updatedServiceCredentials = Map.of("newTestCredentialsKey", "newTestCredentialsValue");
        String updatedSyslogDrainUrl = "https://newSyslogDrain.com";
        List<String> updatedTags = List.of("tag1", "tag2");
        try {
            client.createUserProvidedServiceInstance(buildUserProvidedService(serviceName));

            client.updateServiceParameters(serviceName, updatedServiceCredentials);

            client.updateServiceSyslogDrainUrl(serviceName, updatedSyslogDrainUrl);

            client.updateServiceTags(serviceName, updatedTags);

            CloudServiceInstance service = client.getServiceInstance(serviceName);
            Map<String, Object> serviceCredentials = client.getUserProvidedServiceInstanceParameters(service.getGuid());

            assertEquals(updatedSyslogDrainUrl, service.getSyslogDrainUrl());
            assertEquals(updatedServiceCredentials, serviceCredentials);
            assertTrue(service.getTags()
                              .containsAll(updatedTags),
                       MessageFormat.format("Expected tags \"{0}\" but was \"{1}\"", updatedTags, service.getTags()));
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteServiceInstance(serviceName);
        }
    }

    static Stream<Arguments> createManagedService() {
        return Stream.of(
                         // (1) Without specified broker name
                         Arguments.of("test-managed-service", null),
                         // (2) With specified broker name
                         Arguments.of("test-managed-service-with-broker", "test-service-broker"));
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Create a managed service")
    void createManagedService(String serviceName, String brokerName) {
        if (!pushedServiceBroker) {
            return;
        }
        try {
            client.createServiceInstance(ImmutableCloudServiceInstance.builder()
                                                                      .name(serviceName)
                                                                      .label(IntegrationTestConstants.SERVICE_OFFERING)
                                                                      .plan(IntegrationTestConstants.SERVICE_PLAN)
                                                                      .broker(brokerName)
                                                                      .build());
            pollLastOperationServiceInstanceState(serviceName);
            CloudServiceInstance service = client.getServiceInstance(serviceName);
            assertEquals(serviceName, service.getName());
            assertEquals(IntegrationTestConstants.SERVICE_OFFERING, service.getLabel());
            assertEquals(IntegrationTestConstants.SERVICE_PLAN, service.getPlan());
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteServiceInstance(serviceName);
            verifyServiceIsOrBeingDeleted(serviceName);
        }
    }

    @Test
    @DisplayName("Update managed service")
    void updateManagedService() {
        if (!pushedServiceBroker) {
            return;
        }
        String serviceName = "test-service";
        Map<String, Object> parameters = Map.of("test-key", "test-value", "test-key-2", "test-value-2");
        List<String> serviceTags = List.of("test", "prod");

        try {
            client.createServiceInstance(ImmutableCloudServiceInstance.builder()
                                                                      .name(serviceName)
                                                                      .label(IntegrationTestConstants.SERVICE_OFFERING)
                                                                      .plan(IntegrationTestConstants.SERVICE_PLAN)
                                                                      .build());
            pollLastOperationServiceInstanceState(serviceName);

            client.updateServicePlan(serviceName, IntegrationTestConstants.SERVICE_PLAN_2);
            pollLastOperationServiceInstanceState(serviceName);

            client.updateServiceParameters(serviceName, parameters);
            pollLastOperationServiceInstanceState(serviceName);

            client.updateServiceTags(serviceName, serviceTags);
            pollLastOperationServiceInstanceState(serviceName);

            CloudServiceInstance service = client.getServiceInstance(serviceName);
            Map<String, Object> resultParameters = client.getServiceInstanceParameters(service.getGuid());

            assertEquals(serviceName, service.getName());
            assertEquals(IntegrationTestConstants.SERVICE_OFFERING, service.getLabel());
            assertEquals(IntegrationTestConstants.SERVICE_PLAN_2, service.getPlan());
            assertEquals(parameters, resultParameters);
            assertTrue(service.getTags()
                              .containsAll(serviceTags),
                       MessageFormat.format("Expected tags \"{0}\" but was \"{1}\"", serviceTags, service.getTags()));
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteServiceInstance(serviceName);
            verifyServiceIsOrBeingDeleted(serviceName);
        }
    }

    private void pollLastOperationServiceInstanceState(String serviceInstanceName) {
        int times = 0;
        ServiceOperation lastOperation = client.getServiceInstance(serviceInstanceName)
                                               .getLastOperation();
        while (!lastOperation.getState()
                             .equals(ServiceOperation.State.SUCCEEDED)) {
            sleep(TimeUnit.SECONDS, 1);
            lastOperation = client.getServiceInstance(serviceInstanceName)
                                  .getLastOperation();
            if (lastOperation.getState()
                             .equals(ServiceOperation.State.FAILED)) {
                System.err.println(JsonUtil.convertToJson(lastOperation));
                throw new IllegalStateException(String.format("Service operation failed for service: %s", serviceInstanceName));
            }
            if (times++ > 60) {
                System.err.println(JsonUtil.convertToJson(lastOperation));
                throw new IllegalStateException(String.format("Service operation timeout exceeded for service: %s", serviceInstanceName));
            }
        }
    }

    static Stream<Arguments> getServiceInstance() {
        return Stream.of(Arguments.of("test-service", true, null, true),
                         Arguments.of("not-exist", true, CloudOperationException.class, false),
                         Arguments.of("not-exist-optional", false, null, false));
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Get service instance")
    void getServiceInstance(String serviceName, boolean required, Class<? extends Exception> expectedException, boolean expectedService) {
        if (!pushedServiceBroker) {
            return;
        }
        String serviceNameToCreate = "test-service";

        try {
            client.createServiceInstance(ImmutableCloudServiceInstance.builder()
                                                                      .name(serviceNameToCreate)
                                                                      .label(IntegrationTestConstants.SERVICE_OFFERING)
                                                                      .plan(IntegrationTestConstants.SERVICE_PLAN)
                                                                      .build());
            pollLastOperationServiceInstanceState(serviceNameToCreate);
            if (expectedException != null) {
                assertThrows(expectedException, () -> client.getServiceInstance(serviceName, required));
                return;
            }
            CloudServiceInstance service = client.getServiceInstance(serviceName, required);
            if (expectedService) {
                assertEquals(serviceName, service.getName());
                return;
            }
            assertNull(service);
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteServiceInstance(serviceNameToCreate);
            verifyServiceIsOrBeingDeleted(serviceNameToCreate);
        }
    }

    @Test
    @DisplayName("Delete service instance")
    void deleteServiceInstance() {
        if (!pushedServiceBroker) {
            return;
        }
        String serviceName = "test-service";

        try {
            client.createServiceInstance(ImmutableCloudServiceInstance.builder()
                                                                      .name(serviceName)
                                                                      .label(IntegrationTestConstants.SERVICE_OFFERING)
                                                                      .plan(IntegrationTestConstants.SERVICE_PLAN)
                                                                      .build());
            pollLastOperationServiceInstanceState(serviceName);
            client.deleteServiceInstance(serviceName);
            verifyServiceIsOrBeingDeleted(serviceName);
        } catch (Exception e) {
            fail(e);
        } finally {
            CloudServiceInstance service = client.getServiceInstance(serviceName, false);
            if (service != null) {
                client.deleteServiceInstance(service);
            }
        }
    }

    private void verifyServiceIsOrBeingDeleted(String serviceName) {
        CloudServiceInstance serviceInstance = client.getServiceInstance(serviceName, false);
        int times = 0;
        while (serviceInstance != null) {
            if (times++ > 30) {
                fail(MessageFormat.format("Timeout when waiting for service deletion, error \"{0}\"", serviceInstance.getLastOperation()
                                                                                                                     .getDescription()));
            }
            sleep(TimeUnit.SECONDS, 1);
            serviceInstance = client.getServiceInstance(serviceName, false);
        }
    }

    @Test
    @DisplayName("Create space scoped service broker")
    void createSpaceScopedServiceBroker() {
        if (!pushedServiceBroker) {
            return;
        }
        String serviceBrokerName = "test-space-scoped-service-broker";
        String defaultDomain = client.getDefaultDomain()
                                     .getName();
        String expectedServiceBrokerUrl = MessageFormat.format("https://{0}.{1}/{2}", IntegrationTestConstants.SERVICE_BROKER_HOST,
                                                               defaultDomain, "configurations/2");

        try {
            createServiceBroker(serviceBrokerName, "configurations/2");

            CloudServiceBroker broker = client.getServiceBroker(serviceBrokerName);
            assertEquals(serviceBrokerName, broker.getName());
            assertEquals(target.getMetadata()
                               .getGuid()
                               .toString(),
                         broker.getSpaceGuid());
            assertEquals(expectedServiceBrokerUrl, broker.getUrl());
        } catch (Exception e) {
            fail(e);
        } finally {
            String jobId = client.deleteServiceBroker(serviceBrokerName);
            pollServiceBrokerOperation(jobId, serviceBrokerName);
        }
    }

    @Test
    @DisplayName("Update space scoped service broker")
    void updateSpaceScopedServiceBroker() {
        if (!pushedServiceBroker) {
            return;
        }
        String serviceBrokerName = "test-space-scoped-service-broker";
        String targetSpaceGuid = target.getMetadata()
                                       .getGuid()
                                       .toString();
        String defaultDomain = client.getDefaultDomain()
                                     .getName();
        String expectedServiceBrokerUrl = MessageFormat.format("https://{0}.{1}/{2}", IntegrationTestConstants.SERVICE_BROKER_HOST,
                                                               defaultDomain, "configurations/3");

        try {
            createServiceBroker(serviceBrokerName, "configurations/2");

            String jobId = client.updateServiceBroker(ImmutableCloudServiceBroker.builder()
                                                                                 .name(serviceBrokerName)
                                                                                 .username("new-user")
                                                                                 .password("new-password")
                                                                                 .url(MessageFormat.format("https://{0}.{1}/{2}",
                                                                                                           IntegrationTestConstants.SERVICE_BROKER_HOST,
                                                                                                           defaultDomain,
                                                                                                           "configurations/3"))
                                                                                 .spaceGuid(targetSpaceGuid)
                                                                                 .build());
            pollServiceBrokerOperation(jobId, serviceBrokerName);

            CloudServiceBroker broker = client.getServiceBroker(serviceBrokerName);
            assertEquals(serviceBrokerName, broker.getName());
            assertEquals(target.getMetadata()
                               .getGuid()
                               .toString(),
                         broker.getSpaceGuid());
            assertEquals(expectedServiceBrokerUrl, broker.getUrl());
        } catch (Exception e) {
            fail(e);
        } finally {
            String jobId = client.deleteServiceBroker(serviceBrokerName);
            pollServiceBrokerOperation(jobId, serviceBrokerName);
        }
    }

    @Test
    @DisplayName("Delete space scoped service broker")
    void deleteSpaceScopedServiceBroker() {
        if (!pushedServiceBroker) {
            return;
        }
        String serviceBrokerName = "test-space-scoped-service-broker";

        try {
            createServiceBroker(serviceBrokerName, "configurations/2");

            String jobId = client.deleteServiceBroker(serviceBrokerName);
            pollServiceBrokerOperation(jobId, serviceBrokerName);

            assertThrows(CloudOperationException.class, () -> client.getServiceBroker(serviceBrokerName));
        } catch (Exception e) {
            fail(e);
        } finally {
            CloudServiceBroker broker = client.getServiceBroker(serviceBrokerName, false);
            if (broker != null) {
                String jobId = client.deleteServiceBroker(serviceBrokerName);
                pollServiceBrokerOperation(jobId, serviceBrokerName);
            }
        }
    }

    @Test
    void testFetchingOfFailedServiceKey() {
        if (!pushedServiceBroker) {
            return;
        }
        String testServiceInstanceName = "service-instance-with-failed-service-keys";
        try {
            client.createServiceInstance(ImmutableCloudServiceInstance.builder()
                                                                      .name(testServiceInstanceName)
                                                                      .label(IntegrationTestConstants.SERVICE_OFFERING)
                                                                      .plan(IntegrationTestConstants.SERVICE_PLAN)
                                                                      .build());
            pollLastOperationServiceInstanceState(testServiceInstanceName);
            CloudServiceInstance serviceInstance = client.getServiceInstance(testServiceInstanceName);
            createServiceKeySync(testServiceInstanceName, "successful-key", Map.of("test-key", "test-value"));
            FailConfiguration failConfiguration = ImmutableFailConfiguration.builder()
                                                                            .operationType(FailConfiguration.OperationType.CREATE_SERVICE_KEY.toString())
                                                                            .addInstanceId(serviceInstance.getGuid())
                                                                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                                            .build();
            configureServiceBroker(List.of(failConfiguration));
            createServiceKeySilently(testServiceInstanceName, "failed-key", Collections.emptyMap());
            List<CloudServiceKey> serviceKeys = client.getServiceKeysWithCredentials(testServiceInstanceName);
            assertEquals(2, serviceKeys.size());
            assertEquals(Map.of("test-key", "test-value"), findServiceKeyByName("successful-key", serviceKeys).getCredentials());
            assertEquals(Collections.emptyMap(), findServiceKeyByName("failed-key", serviceKeys).getCredentials());
        } catch (Exception e) {
            fail(e);
        } finally {
            List<CloudServiceKey> serviceKeys = client.getServiceKeys(testServiceInstanceName);
            deleteServiceKeys(serviceKeys);
            client.deleteServiceInstance(testServiceInstanceName);
            verifyServiceIsOrBeingDeleted(testServiceInstanceName);
        }
    }

    private void deleteServiceKeys(List<CloudServiceKey> serviceKeys) {
        serviceKeys.parallelStream()
                   .map(serviceKey -> client.deleteServiceBinding(serviceKey.getGuid()))
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .forEach(this::waitAsyncJobToComplete);
    }

    private void waitAsyncJobToComplete(String jobId) {
        CloudAsyncJob asyncJob = client.getAsyncJob(jobId);
        JobState state = asyncJob.getState();
        while (state == JobState.PROCESSING || state == JobState.POLLING) {
            asyncJob = client.getAsyncJob(jobId);
            state = asyncJob.getState();
            sleep(TimeUnit.SECONDS, 1);
        }
        if (state == JobState.FAILED) {
            throw new IllegalStateException(MessageFormat.format("Error while polling service key job \"{0}\"", asyncJob.getErrors()));
        }
    }

    private static void sleep(TimeUnit timeUnit, int value) {
        try {
            Thread.sleep(timeUnit.toMillis(value));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void configureServiceBroker(List<FailConfiguration> failConfigurations) {
        ServiceBrokerConfiguration serviceBrokerConfiguration = ImmutableServiceBrokerConfiguration.builder()
                                                                                                   .asyncDurationForServiceCredentialBindingsInMillis(100)
                                                                                                   .failConfigurations(failConfigurations)
                                                                                                   .build();
        String serviceBrokerUrl = getServiceBrokerUrl(SERVICE_BROKER_ENDPOINT, client.getDefaultDomain()
                                                                                     .getName());
        WebClient.create()
                 .put()
                 .uri(serviceBrokerUrl)
                 .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                 .body(BodyInserters.fromValue(serviceBrokerConfiguration))
                 .retrieve()
                 .toBodilessEntity()
                 .block();
    }

    private void createServiceKeySilently(String serviceInstanceName, String serviceKeyName, Map<String, Object> serviceKeyCredentials) {
        try {
            createServiceKeySync(serviceInstanceName, serviceKeyName, serviceKeyCredentials);
        } catch (Exception e) {
            // Do nothing
        }
    }

    private void createServiceKeySync(String serviceInstanceName, String serviceKeyName, Map<String, Object> serviceKeyCredentials) {
        Optional<String> jobId = client.createServiceKey(serviceInstanceName, serviceKeyName, serviceKeyCredentials);
        jobId.ifPresent(this::waitAsyncJobToComplete);
    }

    private CloudServiceKey findServiceKeyByName(String keyName, List<CloudServiceKey> serviceKeys) {
        return serviceKeys.stream()
                          .filter(serviceKey -> keyName.equals(serviceKey.getName()))
                          .findFirst()
                          .orElseThrow(() -> new IllegalStateException(MessageFormat.format("Service key with name: \"{0}\" not found!",
                                                                                            keyName)));
    }

    private static void pushServiceBrokerApplication(Path brokerPath) throws InterruptedException {
        client.createApplication(getServiceBrokerApplicationToCreate());
        CloudPackage cloudPackage = ApplicationUtil.uploadApplication(client, IntegrationTestConstants.SERVICE_BROKER_APP_NAME, brokerPath);
        ApplicationUtil.stageApplication(client, IntegrationTestConstants.SERVICE_BROKER_APP_NAME, cloudPackage);
        ApplicationUtil.startApplication(client, IntegrationTestConstants.SERVICE_BROKER_APP_NAME);
    }

    private static ApplicationToCreateDto getServiceBrokerApplicationToCreate() {
        String defaultDomain = client.getDefaultDomain()
                                     .getName();
        Staging staging = ImmutableStaging.builder()
                                          .addBuildpack(IntegrationTestConstants.JAVA_BUILDPACK)
                                          .build();
        Set<CloudRoute> routes = Set.of(ImmutableCloudRoute.builder()
                                                           .host(IntegrationTestConstants.SERVICE_BROKER_HOST)
                                                           .domain(ImmutableCloudDomain.builder()
                                                                                       .name(defaultDomain)
                                                                                       .build())
                                                           .url(APPLICATION_HOST + "." + defaultDomain)
                                                           .build());
        Map<String, String> appEnv = getServiceBrokerEnvConfiguration();
        return ImmutableApplicationToCreateDto.builder()
                                              .name(IntegrationTestConstants.SERVICE_BROKER_APP_NAME)
                                              .staging(staging)
                                              .diskQuotaInMb(IntegrationTestConstants.SERVICE_BROKER_DISK_IN_MB)
                                              .memoryInMb(IntegrationTestConstants.SERVICE_BROKER_MEMORY_IN_MB)
                                              .routes(routes)
                                              .env(appEnv)
                                              .build();
    }

    private static Map<String, String> getServiceBrokerEnvConfiguration() {
        URL url = ServicesCloudControllerClientIntegrationTest.class.getResource(IntegrationTestConstants.SERVICE_BROKER_ENV_CONTENT);
        String configuration;
        try {
            configuration = Files.readString(Paths.get(url.toURI()));
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(e);
        }
        return JsonUtil.convertJsonToMap(configuration)
                       .entrySet()
                       .stream()
                       .collect(Collectors.toMap(Map.Entry::getKey,
                                                 ServicesCloudControllerClientIntegrationTest::convertMapEntryValueToString));

    }

    private static String convertMapEntryValueToString(Map.Entry<String, Object> entry) {
        if (entry.getValue() instanceof String) {
            return (String) entry.getValue();
        }
        return JsonUtil.convertToJson(entry.getValue(), true);
    }

    private static void createServiceBroker(String serviceBrokerName, String serviceBrokerEndpoint) {
        String defaultDomain = client.getDefaultDomain()
                                     .getName();
        String targetSpaceGuid = target.getMetadata()
                                       .getGuid()
                                       .toString();
        String jobId = client.createServiceBroker(ImmutableCloudServiceBroker.builder()
                                                                             .name(serviceBrokerName)
                                                                             .username(IntegrationTestConstants.SERVICE_BROKER_USERNAME)
                                                                             .password(IntegrationTestConstants.SERVICE_BROKER_PASSWORD)
                                                                             .url(getServiceBrokerUrl(serviceBrokerEndpoint, defaultDomain))
                                                                             .spaceGuid(targetSpaceGuid)
                                                                             .build());
        pollServiceBrokerOperation(jobId, serviceBrokerName);
    }

    private static String getServiceBrokerUrl(String serviceBrokerEndpoint, String domain) {
        return MessageFormat.format("https://{0}.{1}/{2}", IntegrationTestConstants.SERVICE_BROKER_HOST, domain, serviceBrokerEndpoint);
    }

    private static void pollServiceBrokerOperation(String jobId, String serviceBrokerName) {
        CloudAsyncJob job = client.getAsyncJob(jobId);
        while (job.getState() != JobState.COMPLETE && !hasAsyncJobFailed(job)) {
            sleep(TimeUnit.SECONDS, 1);
            job = client.getAsyncJob(jobId);
        }
        if (hasAsyncJobFailed(job)) {
            fail(MessageFormat.format("Polling async operation of service broker \"{0}\" failed with \"{1}\"", serviceBrokerName,
                                      job.getErrors()));
        }
    }

    private static boolean hasAsyncJobFailed(CloudAsyncJob job) {
        return job.getState() == JobState.FAILED;
    }

    private CloudServiceInstance buildUserProvidedService(String serviceName) {
        return ImmutableCloudServiceInstance.builder()
                                            .name(serviceName)
                                            .type(ServiceInstanceType.USER_PROVIDED)
                                            .credentials(USER_SERVICE_CREDENTIALS)
                                            .syslogDrainUrl(SYSLOG_DRAIN_URL)
                                            .tags(SERVICE_TAGS)
                                            .build();
    }

}

package org.cloudfoundry.multiapps.controller.client.facade;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.JobState;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ImmutableApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.util.JsonUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ServiceBindingsCloudControllerClientIntegrationTest extends CloudControllerClientIntegrationTest {

    private static final String APP_NAME = "binding-app-test";
    private static final String SERVICE_NAME = "binding-service-test";
    private static final String BINDING_NAME = "binding-test";
    private static final String SERVICE_BROKER_ENDPOINT = "configurations/1";
    private static final int MAX_POLL_ATTEMPTS = 60;
    private static final int MAX_SERVICE_DELETION_POLL_ATTEMPTS = 30;

    private static final ApplicationServicesUpdateCallback FAIL_ON_ERROR = (cloudOperationException, applicationName, serviceInstanceName) -> {
        throw cloudOperationException;
    };

    private static boolean pushedServiceBroker = false;

    @BeforeAll
    static void setUp() throws InterruptedException {
        String brokerPathString = ITVariable.PATH_TO_SERVICE_BROKER_APPLICATION.getValue();
        if (brokerPathString == null) {
            return;
        }

        Path brokerPath = Paths.get(brokerPathString);
        if (Files.notExists(brokerPath)) {
            fail(MessageFormat.format("Specified service broker path \"{0}\" does not exists", brokerPathString));
        }
        pushServiceBrokerApplication(brokerPath);

        try {
            createServiceBroker();
            pushedServiceBroker = true;
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                // Ignore because a service broker with this name already exists
                pushedServiceBroker = true;
            } else {
                fail("Service broker creation failed!", e);
            }
        }

        if (pushedServiceBroker) {
            pushApplication();
        }
    }

    @AfterAll
    static void tearDown() {
        if (!pushedServiceBroker) {
            return;
        }
        client.deleteApplication(APP_NAME);
        String jobId = client.deleteServiceBroker(IntegrationTestConstants.SERVICE_BROKER_NAME);
        pollServiceBrokerOperation(jobId);
        client.deleteApplication(IntegrationTestConstants.SERVICE_BROKER_APP_NAME);
        client.deleteRoute(IntegrationTestConstants.SERVICE_BROKER_HOST, client.getDefaultDomain()
                                                                               .getName(),
                           null);
    }

    @Test
    @DisplayName("Bind a service instance to an application and list the binding using all the different ways")
    void bindServiceInstanceAndListBindings() {
        if (!pushedServiceBroker) {
            return;
        }
        try {
            createManagedService();
            UUID appGuid = client.getApplicationGuid(APP_NAME);
            UUID serviceInstanceGuid = client.getServiceInstance(SERVICE_NAME)
                                             .getGuid();

            Optional<String> bindingJob = client.bindServiceInstance(BINDING_NAME, APP_NAME, SERVICE_NAME);
            bindingJob.ifPresent(job -> waitAsyncJobToComplete(job));

            List<CloudServiceBinding> appBindings = client.getAppBindings(appGuid);
            assertEquals(1, appBindings.size());
            assertEquals(serviceInstanceGuid, appBindings.getFirst()
                                                         .getServiceInstanceGuid());

            List<CloudServiceBinding> serviceAppBindings = client.getServiceAppBindings(serviceInstanceGuid);
            assertEquals(1, serviceAppBindings.size());
            assertEquals(appGuid, serviceAppBindings.getFirst()
                                                    .getApplicationGuid());

            List<CloudServiceBinding> bindingsForApp = client.getServiceBindingsForApplication(appGuid, serviceInstanceGuid);
            assertEquals(1, bindingsForApp.size());

            UUID bindingGuid = appBindings.getFirst()
                                          .getGuid();
            CloudServiceBinding binding = client.getServiceBinding(bindingGuid);
            assertNotNull(binding);
            assertEquals(appGuid, binding.getApplicationGuid());
            assertEquals(serviceInstanceGuid, binding.getServiceInstanceGuid());
        } catch (Exception e) {
            fail(e);
        } finally {
            unbindServiceInstances();
            deleteService();
        }
    }

    @Test
    @DisplayName("Unbind a service instance and verify the binding is removed")
    void unbindServiceInstance() {
        if (!pushedServiceBroker) {
            return;
        }
        try {
            createManagedService();
            UUID appGuid = client.getApplicationGuid(APP_NAME);

            Optional<String> bindingJob = client.bindServiceInstance(BINDING_NAME, APP_NAME, SERVICE_NAME);
            bindingJob.ifPresent(job -> waitAsyncJobToComplete(job));
            List<CloudServiceBinding> appBindings = client.getAppBindings(appGuid);
            assertEquals(1, appBindings.size());

            List<String> unbindJobs = client.unbindServiceInstance(APP_NAME, SERVICE_NAME);
            unbindJobs.forEach(job -> waitAsyncJobToComplete(job));

            assertTrue(client.getAppBindings(appGuid)
                             .isEmpty());
        } catch (Exception e) {
            fail(e);
        } finally {
            deleteService();
        }
    }

    @Test
    @DisplayName("Unbinding operation when no binding exists throws a CloudOperationException")
    void unbindWhenNoBindingExistsThrows() {
        if (!pushedServiceBroker) {
            return;
        }
        try {
            createManagedService();
            assertThrows(CloudOperationException.class, () -> client.unbindServiceInstance(APP_NAME, SERVICE_NAME));
        } catch (Exception e) {
            fail(e);
        } finally {
            deleteService();
        }
    }

    @Test
    @DisplayName("An application with no bindings returns an empty binding list")
    void appWithoutBindingsReturnsEmptyList() {
        if (!pushedServiceBroker) {
            return;
        }

        UUID appGuid = client.getApplicationGuid(APP_NAME);
        assertTrue(client.getAppBindings(appGuid)
                         .isEmpty());
    }

    @Test
    @DisplayName("Set service binding metadata and then unset the label by sending a null value")
    void updateServiceBindingMetadata() {
        if (!pushedServiceBroker) {
            return;
        }

        try {
            createManagedService();
            UUID appGuid = client.getApplicationGuid(APP_NAME);

            Optional<String> bindingJob = client.bindServiceInstance(BINDING_NAME, APP_NAME, SERVICE_NAME);
            bindingJob.ifPresent(job -> waitAsyncJobToComplete(job));
            UUID bindingGuid = client.getAppBindings(appGuid)
                                     .getFirst()
                                     .getGuid();

            client.updateServiceBindingMetadata(bindingGuid, ImmutableMetadata.builder()
                                                                              .putLabel("integration-test-label", "bindings-coverage")
                                                                              .build());

            client.updateServiceBindingMetadata(bindingGuid, ImmutableMetadata.builder()
                                                                              .putLabel("integration-test-label", null)
                                                                              .build());
        } catch (Exception e) {
            fail(e);
        } finally {
            unbindServiceInstances();
            deleteService();
        }
    }

    @Test
    @DisplayName("Get service binding parameters returns the parameters which were given at binding time")
    void getServiceBindingParameters() {
        String bindingParameterKey = "binding-param-key";
        String bindingParameterValue = "binding-param-value";

        if (!pushedServiceBroker) {
            return;
        }

        Map<String, Object> bindParameters = Map.of(bindingParameterKey, bindingParameterValue);

        try {
            createManagedService();
            UUID appGuid = client.getApplicationGuid(APP_NAME);

            Optional<String> bindingJob = client.bindServiceInstance(BINDING_NAME, APP_NAME, SERVICE_NAME, bindParameters, FAIL_ON_ERROR);
            bindingJob.ifPresent(job -> waitAsyncJobToComplete(job));
            UUID bindingGuid = client.getAppBindings(appGuid)
                                     .getFirst()
                                     .getGuid();

            Map<String, Object> parameters = client.getServiceBindingParameters(bindingGuid);
            assertEquals(bindingParameterValue, parameters.get(bindingParameterKey),
                         MessageFormat.format("Expected the binding time parameter to be returned, but actually got \"{0}\"", parameters));
        } catch (Exception e) {
            fail(e);
        } finally {
            unbindServiceInstances();
            deleteService();
        }
    }

    private void createManagedService() {
        client.createServiceInstance(ImmutableCloudServiceInstance.builder()
                                                                  .name(SERVICE_NAME)
                                                                  .label(IntegrationTestConstants.SERVICE_OFFERING)
                                                                  .plan(IntegrationTestConstants.SERVICE_PLAN)
                                                                  .build());
        pollLastOperationServiceInstanceState(SERVICE_NAME);
    }

    private void unbindServiceInstances() {
        try {
            List<String> jobs = client.unbindServiceInstance(APP_NAME, SERVICE_NAME);
            jobs.forEach(job -> waitAsyncJobToComplete(job));
        } catch (Exception e) {
            // empty on purpose
        }
    }

    private void deleteService() {
        try {
            CloudServiceInstance service = client.getServiceInstance(SERVICE_NAME, false);

            if (service != null) {
                client.deleteServiceInstance(service);
                waitForServiceDeletion();
            }
        } catch (Exception e) {
            // empty on purpose
        }
    }

    private void waitForServiceDeletion() {
        for (int attempt = 0; attempt < MAX_SERVICE_DELETION_POLL_ATTEMPTS; attempt++) {
            CloudServiceInstance serviceInstance = client.getServiceInstance(SERVICE_NAME, false);

            if (serviceInstance == null) {
                return;
            }

            MiscUtil.sleep(TimeUnit.SECONDS.toMillis(1));
        }

        CloudServiceInstance serviceInstance = client.getServiceInstance(SERVICE_NAME, false);
        if (serviceInstance != null) {
            fail(MessageFormat.format("Timeout when waiting for service deletion, error \"{0}\"", serviceInstance.getLastOperation()
                                                                                                                 .getDescription()));
        }
    }

    private void pollLastOperationServiceInstanceState(String serviceInstanceName) {
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            ServiceOperation lastOperation = client.getServiceInstance(serviceInstanceName)
                                                   .getLastOperation();

            if (lastOperation.getState()
                             .equals(ServiceOperation.State.SUCCEEDED)) {
                return;
            }

            if (lastOperation.getState()
                             .equals(ServiceOperation.State.FAILED)) {
                System.err.println(JsonUtil.convertToJson(lastOperation));
                throw new IllegalStateException(MessageFormat.format("Service operation failed for service: \"{0}\"", serviceInstanceName));
            }

            MiscUtil.sleep(TimeUnit.SECONDS.toMillis(1));
        }
        System.err.println(JsonUtil.convertToJson(client.getServiceInstance(serviceInstanceName)
                                                        .getLastOperation()));
        throw new IllegalStateException(
            MessageFormat.format("Service operation timeout exceeded for service: \"{0}\"", serviceInstanceName));
    }

    private void waitAsyncJobToComplete(String jobId) {
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            CloudAsyncJob asyncJob = client.getAsyncJob(jobId);
            JobState state = asyncJob.getState();

            if (state == JobState.FAILED) {
                throw new IllegalStateException(MessageFormat.format("Error while polling binding job \"{0}\"", asyncJob.getErrors()));
            }

            if (state != JobState.PROCESSING && state != JobState.POLLING) {
                return;
            }

            MiscUtil.sleep(TimeUnit.SECONDS.toMillis(1));
        }

        throw new IllegalStateException(MessageFormat.format("Timeout while polling job \"{0}\"", jobId));
    }

    private static void pushApplication() throws InterruptedException {
        Staging staging = ImmutableStaging.builder()
                                          .addBuildpack(IntegrationTestConstants.STATICFILE_BUILDPACK)
                                          .build();

        ApplicationToCreateDto applicationDto = ImmutableApplicationToCreateDto.builder()
                                                                               .name(APP_NAME)
                                                                               .staging(staging)
                                                                               .diskQuotaInMb(IntegrationTestConstants.DISK_IN_MB)
                                                                               .memoryInMb(IntegrationTestConstants.MEMORY_IN_MB)
                                                                               .build();

        client.createApplication(applicationDto);
        CloudPackage cloudPackage = ApplicationUtil.uploadApplication(client, APP_NAME, getStaticfileApplicationContentPath());
        ApplicationUtil.stageApplication(client, APP_NAME, cloudPackage);
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
                                                           .url(IntegrationTestConstants.SERVICE_BROKER_HOST + "." + defaultDomain)
                                                           .build());

        return ImmutableApplicationToCreateDto.builder()
                                              .name(IntegrationTestConstants.SERVICE_BROKER_APP_NAME)
                                              .staging(staging)
                                              .diskQuotaInMb(IntegrationTestConstants.SERVICE_BROKER_DISK_IN_MB)
                                              .memoryInMb(IntegrationTestConstants.SERVICE_BROKER_MEMORY_IN_MB)
                                              .routes(routes)
                                              .env(getServiceBrokerEnvConfiguration())
                                              .build();
    }

    private static void createServiceBroker() {
        String defaultDomain = client.getDefaultDomain()
                                     .getName();

        String targetSpaceGuid = target.getMetadata()
                                       .getGuid()
                                       .toString();

        String brokerUrl = MessageFormat.format("https://{0}.{1}/{2}", IntegrationTestConstants.SERVICE_BROKER_HOST, defaultDomain,
                                                SERVICE_BROKER_ENDPOINT);

        String jobId = client.createServiceBroker(ImmutableCloudServiceBroker.builder()
                                                                             .name(IntegrationTestConstants.SERVICE_BROKER_NAME)
                                                                             .username(IntegrationTestConstants.SERVICE_BROKER_USERNAME)
                                                                             .password(IntegrationTestConstants.SERVICE_BROKER_PASSWORD)
                                                                             .url(brokerUrl)
                                                                             .spaceGuid(targetSpaceGuid)
                                                                             .build());
        pollServiceBrokerOperation(jobId);
    }

    private static void pollServiceBrokerOperation(String jobId) {
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            CloudAsyncJob job = client.getAsyncJob(jobId);

            if (job.getState() == JobState.FAILED) {
                fail(MessageFormat.format("Polling async operation of service broker failed with \"{0}\"", job.getErrors()));
            }

            if (job.getState() == JobState.COMPLETE) {
                return;
            }

            MiscUtil.sleep(TimeUnit.SECONDS.toMillis(1));
        }

        fail(MessageFormat.format("Timeout polling the service broker job \"{0}\"", jobId));
    }

}

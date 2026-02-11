package org.cloudfoundry.multiapps.controller.client.facade;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.processes.HealthCheckType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DockerInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDockerInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Status;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ImmutableApplicationToCreateDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.APPLICATION_HOST;
import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.DEFAULT_DOMAIN;
import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.DISK_IN_MB;
import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.HEALTH_CHECK_ENDPOINT;
import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.HEALTH_CHECK_TIMEMOUT;
import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.JAVA_BUILDPACK;
import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.JAVA_BUILDPACK_URL;
import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.MEMORY_IN_MB;
import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.NODEJS_BUILDPACK;
import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.NODEJS_BUILDPACK_URL;
import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.STATICFILE_APPLICATION_CONTENT;
import static org.cloudfoundry.multiapps.controller.client.facade.IntegrationTestConstants.STATICFILE_BUILDPACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ApplicationsCloudControllerClientIntegrationTest extends CloudControllerClientIntegrationTest {

    private static final Path STATICFILE_APPLICATION_PATH = getStaticfileApplicationContentPath();

    @BeforeAll
    static void createDefaultDomain() {
        client.addDomain(DEFAULT_DOMAIN);
    }

    @BeforeAll
    static void deleteExistingApps() {
        var allApps = client.getApplications();
        for (var app : allApps) {
            client.deleteApplication(app.getName());
        }
    }

    @AfterAll
    static void deleteDefaultDomain() {
        List<CloudRoute> routes = client.getRoutes(DEFAULT_DOMAIN);
        for (CloudRoute route : routes) {
            client.deleteRoute(route.getHost(), DEFAULT_DOMAIN, null);
        }
        client.deleteDomain(DEFAULT_DOMAIN);
    }

    @Test
    @DisplayName("Create application and verify its attributes")
    void createApplication() {
        String applicationName = "test-app-1";
        Staging staging = ImmutableStaging.builder()
                                          .addBuildpack(JAVA_BUILDPACK)
                                          .healthCheckType(HealthCheckType.PROCESS.getValue())
                                          .healthCheckHttpEndpoint(HEALTH_CHECK_ENDPOINT)
                                          .healthCheckTimeout(HEALTH_CHECK_TIMEMOUT)
                                          .build();
        CloudRoute route = getImmutableCloudRoute();
        try {
            verifyApplicationWillBeCreated(applicationName, staging, Set.of(route));
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create, delete and verify the application is deleted")
    void deleteApplication() {
        String applicationName = "test-application-2";

        try {
            createAndVerifyDefaultApplication(applicationName);
            client.deleteApplication(applicationName);
            Exception exception = assertThrows(CloudOperationException.class, () -> client.getApplication(applicationName));
            assertTrue(exception.getMessage()
                                .contains(HttpStatus.NOT_FOUND.getReasonPhrase()));
        } catch (Exception e) {
            fail(e);
        } finally {
            CloudApplication app = client.getApplication(applicationName, false);
            if (app != null) {
                client.deleteApplication(applicationName);
            }
        }
    }

    @Test
    @DisplayName("Create application and verify its GUID")
    void getApplicationGuid() {
        String applicationName = "test-application-3";
        try {
            createAndVerifyDefaultApplication(applicationName);
            assertNotNull(client.getApplicationGuid(applicationName));
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application, update and update its environment")
    void testApplicationEnvironment() {
        String applicationName = "test-application-4";
        try {
            createAndVerifyDefaultApplication(applicationName);
            client.updateApplicationEnv(applicationName, Map.of("foo", "bar"));
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            Map<String, String> applicationEnvironment = client.getApplicationEnvironment(applicationGuid);
            assertEquals("bar", applicationEnvironment.get("foo"));
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application, update its healthcheck type and verify it ")
    void updateApplicationHealthcheckType() {
        String applicationName = "test-application-16";
        try {
            createAndVerifyDefaultApplication(applicationName);
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            var processResponse = delegate.applicationsV3()
                                          .getProcess(org.cloudfoundry.client.v3.applications.GetApplicationProcessRequest.builder()
                                                                                                                          .applicationId(
                                                                                                                              applicationGuid.toString())
                                                                                                                          .type("web")
                                                                                                                          .build())
                                          .block();
            delegate.processes()
                    .update(org.cloudfoundry.client.v3.processes.UpdateProcessRequest.builder()
                                                                                     .processId(processResponse.getId())
                                                                                     .healthCheck(
                                                                                         org.cloudfoundry.client.v3.processes.HealthCheck.builder()
                                                                                                                                         .type(
                                                                                                                                             HealthCheckType.NONE)
                                                                                                                                         .build())
                                                                                     .build())
                    .block();
            CloudProcess cloudProcess = client.getApplicationProcess(applicationGuid);
            assertEquals(org.cloudfoundry.multiapps.controller.client.facade.domain.HealthCheckType.NONE,
                         cloudProcess.getHealthCheckType());
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application, upload a package, verify package exists")
    void uploadApplication() {
        String applicationName = "test-application-5";
        try {
            createAndVerifyDefaultApplication(applicationName);
            CloudPackage cloudPackage = ApplicationUtil.uploadApplication(client, applicationName, STATICFILE_APPLICATION_PATH);
            assertEquals(Status.READY, cloudPackage.getStatus());
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application, upload a package, create a build and restart the application")
    void restartApplication() {
        String applicationName = "test-application-6";
        try {
            createAndVerifyDefaultApplication(applicationName);
            CloudPackage cloudPackage = ApplicationUtil.uploadApplication(client, applicationName, STATICFILE_APPLICATION_PATH);
            ApplicationUtil.stageApplication(client, applicationName, cloudPackage);
            client.startApplication(applicationName);
            assertEquals(CloudApplication.State.STARTED, client.getApplication(applicationName)
                                                               .getState());
            client.stopApplication(applicationName);
            assertEquals(CloudApplication.State.STOPPED, client.getApplication(applicationName)
                                                               .getState());
            client.startApplication(applicationName);
            assertEquals(CloudApplication.State.STARTED, client.getApplication(applicationName)
                                                               .getState());
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create, upload, stage start and get instances")
    void getAppInstances() {
        String applicationName = "test-application-7";
        try {
            createAndVerifyDefaultApplication(applicationName);
            CloudPackage cloudPackage = ApplicationUtil.uploadApplication(client, applicationName, STATICFILE_APPLICATION_PATH);
            client.updateApplicationInstances(applicationName, 3);
            ApplicationUtil.stageApplication(client, applicationName, cloudPackage);
            client.startApplication(applicationName);
            CloudApplication application = client.getApplication(applicationName);
            InstancesInfo applicationInstances = client.getApplicationInstances(application);
            assertEquals(3, applicationInstances.getInstances()
                                                .size());
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create an application, rename it, and verify its GUID")
    void renameApplication() {
        String applicationName = "test-application-8";
        String newApplicationName = "new-test-application-8";
        try {
            createAndVerifyDefaultApplication(applicationName);
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            client.rename(applicationName, newApplicationName);
            assertEquals(applicationGuid, client.getApplication(newApplicationName)
                                                .getGuid());
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(newApplicationName);
        }
    }

    @Test
    @DisplayName("Create app and update its memory")
    void updateApplicationMemory() {
        String applicationName = "test-application-9";
        try {
            createAndVerifyDefaultApplication(applicationName);
            client.updateApplicationMemory(applicationName, 256);
            CloudApplication application = client.getApplication(applicationName);
            var process = client.getApplicationProcess(application.getGuid());
            assertEquals(256, process.getMemoryInMb());
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create app and update its staging")
    void updateApplicationStaging() {
        String applicationName = "test-application-10";
        try {
            createAndVerifyDefaultApplication(applicationName);
            client.updateApplicationStaging(applicationName, ImmutableStaging.builder()
                                                                             .command("echo 1")
                                                                             .build());
            CloudApplication application = client.getApplication(applicationName);
            var process = client.getApplicationProcess(application.getGuid());
            assertEquals("echo 1", process.getCommand());
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application with docker package")
    void createDockerPackage() {
        String applicationName = "test-application-11";
        DockerInfo dockerInfo = ImmutableDockerInfo.builder()
                                                   .image("test/image")
                                                   .build();
        try {
            verifyApplicationWillBeCreated(applicationName, ImmutableStaging.builder()
                                                                            .dockerInfo(dockerInfo)
                                                                            .build(), Set.of(getImmutableCloudRoute()));
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            CloudPackage dockerPackage = client.createDockerPackage(applicationGuid, dockerInfo);
            assertEquals(CloudPackage.Type.DOCKER, dockerPackage.getType());
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application and update its routes")
    void updateApplicationRoutes() {
        String applicationName = "test-application-12";
        Set<CloudRoute> newRoutes = Set.of(ImmutableCloudRoute.builder()
                                                              .host("test-application-hostname-modified")
                                                              .url("test-application-hostname-modified." + DEFAULT_DOMAIN)
                                                              .domain(ImmutableCloudDomain.builder()
                                                                                          .name(DEFAULT_DOMAIN)
                                                                                          .build())
                                                              .build());
        try {
            createAndVerifyDefaultApplication(applicationName);
            client.updateApplicationRoutes(applicationName, newRoutes);
            CloudApplication application = client.getApplication(applicationName);
            var routes = client.getApplicationRoutes(application.getGuid());
            assertEquals(List.copyOf(newRoutes), routes);
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application with docker package and check packages")
    void getPackagesForApplication() {
        String applicationName = "test-application-13";
        DockerInfo dockerInfo = ImmutableDockerInfo.builder()
                                                   .image("test/image")
                                                   .build();
        try {
            verifyApplicationWillBeCreated(applicationName, ImmutableStaging.builder()
                                                                            .dockerInfo(dockerInfo)
                                                                            .build(), Set.of(getImmutableCloudRoute()));
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            CloudPackage dockerPackage = client.createDockerPackage(applicationGuid, dockerInfo);
            List<CloudPackage> packagesForApplication = client.getPackagesForApplication(applicationGuid);
            assertTrue(packagesForApplication.stream()
                                             .map(CloudPackage::getGuid)
                                             .anyMatch(packageGuid -> packageGuid.equals(dockerPackage.getGuid())));
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application, upload a package, create a build and test its builds")
    void getBuildsForApplication() {
        String applicationName = "test-application-14";
        try {
            createAndVerifyDefaultApplication(applicationName);
            CloudPackage cloudPackage = ApplicationUtil.uploadApplication(client, applicationName, STATICFILE_APPLICATION_PATH);
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            CloudBuild build = ApplicationUtil.createBuildForPackage(client, cloudPackage);
            List<CloudBuild> buildsForApplication = client.getBuildsForApplication(applicationGuid);
            assertTrue(buildsForApplication.stream()
                                           .map(CloudBuild::getMetadata)
                                           .map(CloudMetadata::getGuid)
                                           .anyMatch(buildGuid -> buildGuid.equals(build.getGuid())));
            assertEquals(build.getMetadata()
                              .getGuid(), client.getBuild(build.getMetadata()
                                                               .getGuid())
                                                .getGuid());
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application and update its metadata")
    void updateApplicationMetadata() {
        String applicationName = "test-application-15";
        Metadata metadata = Metadata.builder()
                                    .label("test-app", "test-app")
                                    .build();
        try {
            createAndVerifyDefaultApplication(applicationName);
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            client.updateApplicationMetadata(applicationGuid, metadata);
            List<CloudApplication> applicationsByMetadataLabelSelector = client.getApplicationsByMetadataLabelSelector("test-app");
            assertEquals(1, applicationsByMetadataLabelSelector.size());
            assertEquals(applicationName, applicationsByMetadataLabelSelector.get(0)
                                                                             .getName());
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application with multiple buildpacks")
    void createApplicationWithBuildpacks() {
        String applicationName = "test-app-16";
        List<String> buildpacks = List.of(JAVA_BUILDPACK, NODEJS_BUILDPACK, STATICFILE_BUILDPACK);
        Staging staging = ImmutableStaging.builder()
                                          .addAllBuildpacks(buildpacks)
                                          .build();
        try {
            verifyApplicationWillBeCreated(applicationName, staging, Set.of(getImmutableCloudRoute()));
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application with CNB lifecycle and verify attributes")
    void createCnbApplication() {
        String applicationName = "test-app-17";
        List<String> buildpacks = List.of(JAVA_BUILDPACK_URL, NODEJS_BUILDPACK_URL);

        Staging staging = ImmutableStaging.builder()
                                          .lifecycleType(LifecycleType.CNB)
                                          .addAllBuildpacks(buildpacks)
                                          .build();
        CloudRoute route = getImmutableCloudRoute();
        try {
            verifyApplicationWillBeCreated(applicationName, staging, Set.of(route));
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Crete application with enabled SSH and vcap-file-based services feature")
    void createApplicationWithSshAndVcapFileBasedServices() {
        String applicationName = "test-app-18";
        Staging staging = ImmutableStaging.builder()
                                          .appFeatures(Map.of("file-based-vcap-services", true, "ssh", true))
                                          .build();
        try {
            verifyApplicationWillBeCreated(applicationName, staging, Set.of(getImmutableCloudRoute()));
        } catch (Exception e) {
            fail(e);
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    private void verifyApplicationWillBeCreated(String applicationName, Staging staging, Set<CloudRoute> routes) {
        ApplicationToCreateDto applicationToCreateDto = ImmutableApplicationToCreateDto.builder()
                                                                                       .name(applicationName)
                                                                                       .staging(staging)
                                                                                       .diskQuotaInMb(DISK_IN_MB)
                                                                                       .memoryInMb(MEMORY_IN_MB)
                                                                                       .routes(routes)
                                                                                       .build();
        client.createApplication(applicationToCreateDto);
        assertApplicationExists(ImmutableCloudApplication.builder()
                                                         .name(applicationName)
                                                         .state(CloudApplication.State.STOPPED)
                                                         .lifecycle(createLifecycle(staging))
                                                         .build(), staging, routes);
    }

    private static void assertApplicationExists(CloudApplication cloudApplication, Staging staging, Set<CloudRoute> routes) {
        CloudApplication application = client.getApplication(cloudApplication.getName());
        var realRoutes = client.getApplicationRoutes(application.getGuid());
        var process = client.getApplicationProcess(application.getGuid());
        assertEquals(cloudApplication.getState(), application.getState());
        assertEquals(cloudApplication.getLifecycle(), application.getLifecycle());
        assertEquals(List.copyOf(routes), realRoutes);
        if (staging.getCommand() != null) {
            assertEquals(staging.getCommand(), process.getCommand());
        }
        assertEquals(MEMORY_IN_MB, process.getMemoryInMb());
        assertEquals(DISK_IN_MB, process.getDiskInMb());
        assertAppFeatures(staging, application);
    }

    private static void assertAppFeatures(Staging staging, CloudApplication application) {
        var appFeatures = client.getApplicationFeatures(application.getGuid());
        for (Map.Entry<String, Boolean> entry : staging.getAppFeatures()
                                                       .entrySet()) {
            String featureName = entry.getKey();
            Boolean expectedValue = entry.getValue();
            Boolean actualValue = appFeatures.get(featureName);
            assertEquals(expectedValue, actualValue);
        }
    }

    private void createAndVerifyDefaultApplication(String applicationName) {
        verifyApplicationWillBeCreated(applicationName, ImmutableStaging.builder()
                                                                        .build(), Set.of(getImmutableCloudRoute()));
    }

    private ImmutableCloudRoute getImmutableCloudRoute() {
        return ImmutableCloudRoute.builder()
                                  .host(APPLICATION_HOST)
                                  .domain(ImmutableCloudDomain.builder()
                                                              .name(DEFAULT_DOMAIN)
                                                              .build())
                                  .url(APPLICATION_HOST + "." + DEFAULT_DOMAIN)
                                  .build();
    }

    private static Path getStaticfileApplicationContentPath() {
        URL url = ApplicationsCloudControllerClientIntegrationTest.class.getResource(STATICFILE_APPLICATION_CONTENT);
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

}

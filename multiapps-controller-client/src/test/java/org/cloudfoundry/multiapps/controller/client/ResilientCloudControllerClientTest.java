package org.cloudfoundry.multiapps.controller.client;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.UploadStatusCallback;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.rest.CloudControllerRestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class ResilientCloudControllerClientTest {

    private CloudControllerRestClient restClient;
    private ResilientCloudControllerClient client;

    @BeforeEach
    void setUp() {
        restClient = Mockito.mock(CloudControllerRestClient.class);
        client = new ResilientCloudControllerClient(restClient);
    }

    @Test
    void testGetTargetIsExecutedWithoutRetryWrapping() {
        CloudSpace space = Mockito.mock(CloudSpace.class);
        Mockito.when(restClient.getTarget())
               .thenReturn(space);

        Assertions.assertSame(space, client.getTarget());
        Mockito.verify(restClient)
               .getTarget();
    }

    @Test
    void testVoidMethodRetriesOnTransientBadGateway() {
        Mockito.doThrow(new CloudOperationException(HttpStatus.BAD_GATEWAY))
               .doNothing()
               .when(restClient)
               .addDomain("example.com");

        client.addDomain("example.com");

        Mockito.verify(restClient, Mockito.times(2))
               .addDomain("example.com");
    }

    @Test
    void testValueReturningMethodRetriesOnTransientBadGateway() {
        CloudApplication app = Mockito.mock(CloudApplication.class);
        Mockito.when(restClient.getApplication("my-app"))
               .thenThrow(new CloudOperationException(HttpStatus.BAD_GATEWAY))
               .thenReturn(app);

        Assertions.assertSame(app, client.getApplication("my-app"));
        Mockito.verify(restClient, Mockito.times(2))
               .getApplication("my-app");
    }

    @Test
    void testNonRetryableStatusPropagatesImmediately() {
        Mockito.when(restClient.getApplication("missing"))
               .thenThrow(new CloudOperationException(HttpStatus.UNAUTHORIZED));

        CloudOperationException thrown = Assertions.assertThrows(CloudOperationException.class, () -> client.getApplication("missing"));

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, thrown.getStatusCode());
        Mockito.verify(restClient, Mockito.times(1))
               .getApplication("missing");
    }

    @Test
    void testGetApplicationsIgnoresNotFound() {
        Mockito.when(restClient.getApplications())
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND))
               .thenReturn(List.of());

        Assertions.assertTrue(client.getApplications()
                                    .isEmpty());
        Mockito.verify(restClient, Mockito.times(2))
               .getApplications();
    }

    @Test
    void testGetDomainsIgnoresNotFound() {
        Mockito.when(restClient.getDomains())
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND))
               .thenReturn(List.<CloudDomain> of());

        Assertions.assertTrue(client.getDomains()
                                    .isEmpty());
        Mockito.verify(restClient, Mockito.times(2))
               .getDomains();
    }

    @Test
    void testRoutesByDomainIgnoresNotFound() {
        Mockito.when(restClient.getRoutes("example.com"))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND))
               .thenReturn(List.of());

        Assertions.assertTrue(client.getRoutes("example.com")
                                    .isEmpty());
        Mockito.verify(restClient, Mockito.times(2))
               .getRoutes("example.com");
    }

    @Test
    void testGetStackByNameDelegates() {
        CloudStack stack = Mockito.mock(CloudStack.class);
        Mockito.when(restClient.getStack("cflinuxfs4"))
               .thenReturn(stack);

        Assertions.assertSame(stack, client.getStack("cflinuxfs4"));
    }

    @Test
    void testGetStackByNameAndRequiredFlagDelegates() {
        CloudStack stack = Mockito.mock(CloudStack.class);
        Mockito.when(restClient.getStack("cflinuxfs4", true))
               .thenReturn(stack);

        Assertions.assertSame(stack, client.getStack("cflinuxfs4", true));
    }

    @Test
    void testRunTaskDelegatesAndRetries() {
        Mockito.when(restClient.runTask(ArgumentMatchers.eq("my-app"), ArgumentMatchers.any()))
               .thenThrow(new CloudOperationException(HttpStatus.BAD_GATEWAY))
               .thenReturn(null);

        client.runTask("my-app", null);

        Mockito.verify(restClient, Mockito.times(2))
               .runTask(ArgumentMatchers.eq("my-app"), ArgumentMatchers.any());
    }

    @Test
    void testCancelTaskDelegates() {
        UUID taskGuid = UUID.randomUUID();

        client.cancelTask(taskGuid);

        Mockito.verify(restClient)
               .cancelTask(taskGuid);
    }

    @Test
    void testRenameDelegates() {
        client.rename("old", "new");

        Mockito.verify(restClient)
               .rename("old", "new");
    }

    @Test
    void testStartApplicationDelegates() {
        client.startApplication("my-app");

        Mockito.verify(restClient)
               .startApplication("my-app");
    }

    @Test
    void testStopApplicationDelegates() {
        client.stopApplication("my-app");

        Mockito.verify(restClient)
               .stopApplication("my-app");
    }

    @Test
    void testRestartApplicationDelegates() {
        client.restartApplication("my-app");

        Mockito.verify(restClient)
               .restartApplication("my-app");
    }

    @Test
    void testBindServiceInstanceReturnsOptional() {
        Mockito.when(restClient.bindServiceInstance("binding", "app", "service"))
               .thenReturn(Optional.of("job-1"));

        Assertions.assertEquals(Optional.of("job-1"), client.bindServiceInstance("binding", "app", "service"));
    }

    @Test
    void testGetApplicationSshEnabledRetriesAndReturnsBoolean() {
        UUID guid = UUID.randomUUID();
        Mockito.when(restClient.getApplicationSshEnabled(guid))
               .thenThrow(new CloudOperationException(HttpStatus.BAD_GATEWAY))
               .thenReturn(true);

        Assertions.assertTrue(client.getApplicationSshEnabled(guid));
        Mockito.verify(restClient, Mockito.times(2))
               .getApplicationSshEnabled(guid);
    }

    @Test
    void testAsyncUploadWithOverrideTimeoutUsesPlainRetry() {
        Path file = Paths.get("/tmp/some.mtar");
        UploadStatusCallback callback = Mockito.mock(UploadStatusCallback.class);
        Duration override = Duration.ofMinutes(2);
        CloudPackage pkg = Mockito.mock(CloudPackage.class);
        Mockito.when(restClient.asyncUploadApplication("my-app", file, callback, override))
               .thenReturn(pkg);

        Assertions.assertSame(pkg, client.asyncUploadApplicationWithExponentialBackoff("my-app", file, callback, override));
        Mockito.verify(restClient)
               .asyncUploadApplication("my-app", file, callback, override);
    }

    @Test
    void testAsyncUploadWithoutOverrideTimeoutUsesExponentialBackoff() {
        Path file = Paths.get("/tmp/some.mtar");
        UploadStatusCallback callback = Mockito.mock(UploadStatusCallback.class);
        CloudPackage pkg = Mockito.mock(CloudPackage.class);
        Mockito.when(restClient.asyncUploadApplication(ArgumentMatchers.eq("my-app"),
                                                       ArgumentMatchers.eq(file),
                                                       ArgumentMatchers.eq(callback),
                                                       ArgumentMatchers.any(Duration.class)))
               .thenReturn(pkg);

        Assertions.assertSame(pkg, client.asyncUploadApplicationWithExponentialBackoff("my-app", file, callback, null));
        Mockito.verify(restClient)
               .asyncUploadApplication(ArgumentMatchers.eq("my-app"),
                                       ArgumentMatchers.eq(file),
                                       ArgumentMatchers.eq(callback),
                                       ArgumentMatchers.any(Duration.class));
    }

    @Test
    void testGetUploadStatusDelegates() {
        UUID packageGuid = UUID.randomUUID();

        client.getUploadStatus(packageGuid);

        Mockito.verify(restClient)
               .getUploadStatus(packageGuid);
    }

    @Test
    void testDeleteOrphanedRoutesIgnoresNotFound() {
        Mockito.doThrow(new CloudOperationException(HttpStatus.NOT_FOUND))
               .doNothing()
               .when(restClient)
               .deleteOrphanedRoutes();

        client.deleteOrphanedRoutes();

        Mockito.verify(restClient, Mockito.times(2))
               .deleteOrphanedRoutes();
    }

    @Test
    void testGetServiceBrokersIgnoresNotFound() {
        Mockito.when(restClient.getServiceBrokers())
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND))
               .thenReturn(List.of());

        Assertions.assertTrue(client.getServiceBrokers()
                                    .isEmpty());
    }

    @Test
    void testGetEventsIgnoresNotFound() {
        Mockito.when(restClient.getEvents())
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND))
               .thenReturn(List.of());

        Assertions.assertTrue(client.getEvents()
                                    .isEmpty());
    }

    @Test
    void testGetServiceOfferingsIgnoresNotFound() {
        Mockito.when(restClient.getServiceOfferings())
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND))
               .thenReturn(List.of());

        Assertions.assertTrue(client.getServiceOfferings()
                                    .isEmpty());
    }

    @Test
    void testGetStacksIgnoresNotFound() {
        Mockito.when(restClient.getStacks())
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND))
               .thenReturn(List.of());

        Assertions.assertTrue(client.getStacks()
                                    .isEmpty());
    }
}
